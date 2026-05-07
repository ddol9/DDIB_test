import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import {
  MAX_VUS,
  OPTION_ID,
  PERFORMANCE_ID,
  SEATS_PER_ORDER,
  WS_CONNECT_TIMEOUT_MS,
  buildSockJsWebSocketUrl,
  createStompFrame,
  fetchSeatCatalog,
  joinQueueAndGetToken,
  login,
  parseStompFrame,
  pickSeatTargets,
  seedUsers,
  sockjsSend,
} from './lib/monolith-common.js';

const ACTIONS_PER_SESSION = __ENV.ACTIONS_PER_SESSION ? parseInt(__ENV.ACTIONS_PER_SESSION, 10) : 4;
const HOLD_MS_MIN = __ENV.HOLD_MS_MIN ? parseInt(__ENV.HOLD_MS_MIN, 10) : 400;
const HOLD_MS_MAX = __ENV.HOLD_MS_MAX ? parseInt(__ENV.HOLD_MS_MAX, 10) : 1500;

const loginSuccess = new Counter('login_success');
const loginFailed = new Counter('login_failed');
const loginTime = new Trend('login_time_ms');
const queueJoined = new Counter('queue_joined');
const queueJoinFailed = new Counter('queue_join_failed');
const tokenIssued = new Counter('token_issued');
const tokenFailed = new Counter('token_failed');
const queueToTokenTime = new Trend('queue_to_token_ms');
const seatRequests = new Counter('seat_requests');
const seatSecured = new Counter('seat_secured');
const seatDenied = new Counter('seat_denied');
const seatReleased = new Counter('seat_released');
const seatSecureRate = new Rate('seat_secure_rate');
const seatResponseTime = new Trend('seat_response_ms');
const raceLost = new Counter('race_lost');
const totalSessions = new Counter('total_sessions');
const wsConnectFailed = new Counter('ws_connect_failed');

const metrics = {
  loginSuccess,
  loginFailed,
  loginTime,
  queueJoined,
  queueJoinFailed,
  tokenIssued,
  tokenFailed,
  queueToTokenTime,
};

export const options = {
  scenarios: {
    seat_war: {
      executor: 'per-vu-iterations',
      vus: MAX_VUS,
      iterations: 1,
      maxDuration: __ENV.MAX_DURATION || '3m',
    },
  },
  thresholds: {
    seat_response_ms: ['p(95)<3000'],
    ws_connect_failed: ['count<50'],
  },
};

export function setup() {
  return {
    userIds: seedUsers(Math.max(MAX_VUS, 10)),
    seatCatalog: fetchSeatCatalog(PERFORMANCE_ID, OPTION_ID),
  };
}

function runSeatSession(queueToken, seatCatalog) {
  totalSessions.add(1);
  const occupied = new Set();
  const sold = new Set();
  const mine = new Set();
  let actions = 0;
  let lockSentAt = 0;
  let activeLockTargets = [];

  const response = ws.connect(buildSockJsWebSocketUrl(queueToken, PERFORMANCE_ID, OPTION_ID), {}, (socket) => {
    let subscriptionId = 0;

    function requestLock() {
      const unavailable = new Set([...occupied, ...sold, ...mine]);
      const targets = pickSeatTargets(seatCatalog.seatIds, unavailable, SEATS_PER_ORDER);
      if (targets.length === 0) {
        socket.close();
        return;
      }
      activeLockTargets = targets;
      seatRequests.add(1);
      lockSentAt = Date.now();
      sockjsSend(socket, createStompFrame('SEND', {
        destination: '/app/seats/lock',
        'content-type': 'application/json',
      }, JSON.stringify({
        performanceId: PERFORMANCE_ID,
        optionId: OPTION_ID,
        seatIds: targets,
      })));
    }

    function requestRelease() {
      if (mine.size === 0) {
        socket.close();
        return;
      }
      const targets = [...mine];
      seatReleased.add(targets.length);
      sockjsSend(socket, createStompFrame('SEND', {
        destination: '/app/seats/release',
        'content-type': 'application/json',
      }, JSON.stringify({
        performanceId: PERFORMANCE_ID,
        optionId: OPTION_ID,
        seatIds: targets,
      })));
    }

    socket.on('message', (data) => {
      if (data === 'o') {
        sockjsSend(socket, createStompFrame('CONNECT', {
          'accept-version': '1.2',
          'heart-beat': '10000,10000',
        }));
        return;
      }
      if (data === 'h' || data.startsWith('c[') || !data.startsWith('a[')) {
        return;
      }
      const frame = parseStompFrame(data);
      if (frame.command === 'CONNECTED') {
        sockjsSend(socket, createStompFrame('SUBSCRIBE', {
          id: `sub-${subscriptionId++}`,
          destination: '/user/queue/seats',
        }));
        sockjsSend(socket, createStompFrame('SUBSCRIBE', {
          id: `sub-${subscriptionId++}`,
          destination: `/topic/seats.${PERFORMANCE_ID}.${OPTION_ID}`,
        }));
        sockjsSend(socket, createStompFrame('SEND', {
          destination: '/app/seats/init',
          'content-type': 'application/json',
        }, '{}'));
        return;
      }
      if (frame.command !== 'MESSAGE') {
        return;
      }
      let message;
      try {
        message = JSON.parse(frame.body);
      } catch (error) {
        return;
      }

      if (message.type === 'INITIAL_STATE') {
        (message.occupiedSeats || []).forEach((seatId) => occupied.add(seatId));
        (message.soldSeats || []).forEach((seatId) => sold.add(seatId));
        requestLock();
        return;
      }
      if (message.type === 'SEAT_LOCKED' && Array.isArray(message.seatIds)) {
        message.seatIds.forEach((seatId) => occupied.add(seatId));
        if (activeLockTargets.length > 0 && activeLockTargets.every((seatId) => message.seatIds.includes(seatId))) {
          seatSecured.add(1);
          seatSecureRate.add(true);
          seatResponseTime.add(Date.now() - lockSentAt);
          activeLockTargets.forEach((seatId) => mine.add(seatId));
          activeLockTargets = [];
          actions += 1;
          if (actions >= ACTIONS_PER_SESSION) {
            socket.close();
            return;
          }
          socket.setTimeout(requestRelease, HOLD_MS_MIN + Math.floor(Math.random() * (HOLD_MS_MAX - HOLD_MS_MIN + 1)));
          return;
        }
      }
      if (message.type === 'SEAT_RELEASED' && Array.isArray(message.seatIds)) {
        message.seatIds.forEach((seatId) => {
          occupied.delete(seatId);
          mine.delete(seatId);
        });
      }
      if (message.type === 'SEAT_SOLD' && Array.isArray(message.seatIds)) {
        message.seatIds.forEach((seatId) => sold.add(seatId));
      }
      if (message.type === 'LOCK_SUCCESS' && Array.isArray(message.seatIds)) {
        seatSecured.add(1);
        seatSecureRate.add(true);
        seatResponseTime.add(Date.now() - lockSentAt);
        message.seatIds.forEach((seatId) => mine.add(seatId));
        activeLockTargets = [];
        actions += 1;
        if (actions >= ACTIONS_PER_SESSION) {
          socket.close();
          return;
        }
        socket.setTimeout(requestRelease, HOLD_MS_MIN + Math.floor(Math.random() * (HOLD_MS_MAX - HOLD_MS_MIN + 1)));
        return;
      }
      if (message.type === 'LOCK_FAILED') {
        seatDenied.add(1);
        seatSecureRate.add(false);
        seatResponseTime.add(Date.now() - lockSentAt);
        raceLost.add(activeLockTargets.length || 1);
        activeLockTargets = [];
        actions += 1;
        if (actions >= ACTIONS_PER_SESSION) {
          socket.close();
          return;
        }
        socket.setTimeout(requestLock, 150 + Math.floor(Math.random() * 350));
      }
    });

    socket.on('error', () => {
      wsConnectFailed.add(1);
    });

    socket.setTimeout(() => socket.close(), WS_CONNECT_TIMEOUT_MS);
  });

  check(response, {
    'realtime websocket connected': (res) => res && res.status === 101,
  });
}

export default function (data) {
  const userId = data.userIds[(__VU - 1) % data.userIds.length];
  const accessToken = login(userId, metrics);
  if (!accessToken) {
    return;
  }
  const queueToken = joinQueueAndGetToken(accessToken, PERFORMANCE_ID, OPTION_ID, metrics);
  if (!queueToken) {
    return;
  }
  runSeatSession(queueToken, data.seatCatalog);
  sleep(0.2);
}

export function handleSummary(data) {
  return {
    stdout: [
      '',
      '=== Monolith Realtime Stress Benchmark ===',
      `seat_requests=${data.metrics.seat_requests?.values.count || 0}`,
      `seat_secured=${data.metrics.seat_secured?.values.count || 0}`,
      `seat_denied=${data.metrics.seat_denied?.values.count || 0}`,
      `seat_response_p95=${data.metrics.seat_response_ms?.values['p(95)'] || 0}`,
      '',
    ].join('\n'),
    'k6/results/realtime-stress-result.json': JSON.stringify(data, null, 2),
  };
}
