import ws from 'k6/ws';
import { sleep } from 'k6';
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

const REENTRY_COUNT = __ENV.REENTRY_COUNT ? parseInt(__ENV.REENTRY_COUNT, 10) : 3;
const HOLD_SECONDS = __ENV.HOLD_SECONDS ? parseFloat(__ENV.HOLD_SECONDS) : 2.5;
const QUEUE_REFRESH_STAGE_1 = __ENV.QUEUE_REFRESH_STAGE_1 || '10s';
const QUEUE_REFRESH_STAGE_2 = __ENV.QUEUE_REFRESH_STAGE_2 || '40s';
const QUEUE_REFRESH_STAGE_3 = __ENV.QUEUE_REFRESH_STAGE_3 || '10s';

const loginSuccess = new Counter('login_success');
const loginFailed = new Counter('login_failed');
const loginTime = new Trend('login_time_ms');
const queueJoined = new Counter('queue_joined');
const queueJoinFailed = new Counter('queue_join_failed');
const tokenIssued = new Counter('token_issued');
const tokenFailed = new Counter('token_failed');
const queueReentry = new Counter('queue_reentry');
const queueToTokenTime = new Trend('queue_to_token_ms');
const shortHoldSuccess = new Rate('short_hold_success_rate');
const tokenExpired = new Counter('token_expired');

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
    queue_refresh: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: QUEUE_REFRESH_STAGE_1, target: Math.max(1, Math.floor(MAX_VUS * 0.5)) },
        { duration: QUEUE_REFRESH_STAGE_2, target: MAX_VUS },
        { duration: QUEUE_REFRESH_STAGE_3, target: 0 },
      ],
    },
  },
  thresholds: {
    queue_to_token_ms: ['p(95)<5000'],
  },
};

export function setup() {
  return {
    userIds: seedUsers(Math.max(MAX_VUS, 10)),
    seatCatalog: fetchSeatCatalog(PERFORMANCE_ID, OPTION_ID),
  };
}

function shortHold(queueToken, seatCatalog) {
  const occupied = new Set();
  const sold = new Set();
  let success = false;
  let activeLockTargets = [];

  ws.connect(buildSockJsWebSocketUrl(queueToken, PERFORMANCE_ID, OPTION_ID), {}, (socket) => {
    let subscriptionId = 0;

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
        const targets = pickSeatTargets(seatCatalog.seatIds, new Set([...occupied, ...sold]), SEATS_PER_ORDER);
        if (targets.length === 0) {
          socket.close();
          return;
        }
        activeLockTargets = targets;
        sockjsSend(socket, createStompFrame('SEND', {
          destination: '/app/seats/lock',
          'content-type': 'application/json',
        }, JSON.stringify({
          performanceId: PERFORMANCE_ID,
          optionId: OPTION_ID,
          seatIds: targets,
        })));
        return;
      }
      if (message.type === 'SEAT_LOCKED' && Array.isArray(message.seatIds)) {
        if (!success && activeLockTargets.length > 0 && activeLockTargets.every((seatId) => message.seatIds.includes(seatId))) {
          success = true;
          socket.setTimeout(() => {
            sockjsSend(socket, createStompFrame('SEND', {
              destination: '/app/seats/release',
              'content-type': 'application/json',
            }, JSON.stringify({
              performanceId: PERFORMANCE_ID,
              optionId: OPTION_ID,
              seatIds: activeLockTargets,
            })));
            sockjsSend(socket, createStompFrame('DISCONNECT'));
            socket.close();
          }, Math.floor(HOLD_SECONDS * 1000));
        }
        return;
      }
      if (message.type === 'LOCK_SUCCESS') {
        success = true;
        socket.setTimeout(() => {
          sockjsSend(socket, createStompFrame('SEND', {
            destination: '/app/seats/release',
            'content-type': 'application/json',
          }, JSON.stringify({
            performanceId: PERFORMANCE_ID,
            optionId: OPTION_ID,
            seatIds: message.seatIds || [],
          })));
          sockjsSend(socket, createStompFrame('DISCONNECT'));
          socket.close();
        }, Math.floor(HOLD_SECONDS * 1000));
        return;
      }
      if (message.type === 'TOKEN_EXPIRED') {
        tokenExpired.add(1);
        socket.close();
      }
    });

    socket.setTimeout(() => socket.close(), WS_CONNECT_TIMEOUT_MS);
  });

  shortHoldSuccess.add(success);
}

export default function (data) {
  const userId = data.userIds[(__VU - 1) % data.userIds.length];
  const accessToken = login(userId, metrics);
  if (!accessToken) {
    return;
  }

  for (let i = 0; i < REENTRY_COUNT; i += 1) {
    if (i > 0) {
      queueReentry.add(1);
    }
    const queueToken = joinQueueAndGetToken(accessToken, PERFORMANCE_ID, OPTION_ID, metrics);
    if (!queueToken) {
      continue;
    }
    shortHold(queueToken, data.seatCatalog);
    sleep(0.4);
  }
}

export function handleSummary(data) {
  return {
    stdout: [
      '',
      '=== Monolith Queue Re-entry Benchmark ===',
      `queue_joined=${data.metrics.queue_joined?.values.count || 0}`,
      `queue_reentry=${data.metrics.queue_reentry?.values.count || 0}`,
      `token_issued=${data.metrics.token_issued?.values.count || 0}`,
      `short_hold_success_rate=${data.metrics.short_hold_success_rate?.values.rate || 0}`,
      '',
    ].join('\n'),
    'k6/results/queue-refresh-result.json': JSON.stringify(data, null, 2),
  };
}
