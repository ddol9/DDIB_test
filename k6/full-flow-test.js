import ws from 'k6/ws';
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend, Rate } from 'k6/metrics';
import {
  API_BASE_URL,
  PERFORMANCE_ID,
  OPTION_ID,
  MAX_VUS,
  SEATS_PER_ORDER,
  WS_CONNECT_TIMEOUT_MS,
  authHeaders,
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

const PAYMENT_KEY_PREFIX = __ENV.PAYMENT_KEY_PREFIX || 'benchmark-payment';
const RESERVATION_POLL_MS = __ENV.RESERVATION_POLL_MS ? parseInt(__ENV.RESERVATION_POLL_MS, 10) : 250;
const RESERVATION_POLL_MAX_MS = __ENV.RESERVATION_POLL_MAX_MS ? parseInt(__ENV.RESERVATION_POLL_MAX_MS, 10) : 5000;
const FULL_FLOW_STAGE_1 = __ENV.FULL_FLOW_STAGE_1 || '10s';
const FULL_FLOW_STAGE_2 = __ENV.FULL_FLOW_STAGE_2 || '10s';
const FULL_FLOW_STAGE_3 = __ENV.FULL_FLOW_STAGE_3 || '40s';
const FULL_FLOW_STAGE_4 = __ENV.FULL_FLOW_STAGE_4 || '10s';

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
const seatResponseTime = new Trend('seat_response_ms');
const paymentPrepareTime = new Trend('payment_prepare_ms');
const paymentConfirmTime = new Trend('payment_confirm_ms');
const reservationProjectionDelay = new Trend('reservation_projection_delay_ms');
const fullFlowSuccess = new Rate('full_flow_success_rate');

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
    full_flow: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: FULL_FLOW_STAGE_1, target: Math.max(1, Math.floor(MAX_VUS * 0.3)) },
        { duration: FULL_FLOW_STAGE_2, target: Math.max(1, Math.floor(MAX_VUS * 0.6)) },
        { duration: FULL_FLOW_STAGE_3, target: MAX_VUS },
        { duration: FULL_FLOW_STAGE_4, target: 0 },
      ],
    },
  },
  thresholds: {
    queue_to_token_ms: ['p(95)<5000'],
    seat_response_ms: ['p(95)<3000'],
    payment_confirm_ms: ['p(95)<2000'],
    reservation_projection_delay_ms: ['p(95)<1000'],
  },
};

export function setup() {
  return {
    userIds: seedUsers(Math.max(MAX_VUS, 10)),
    seatCatalog: fetchSeatCatalog(PERFORMANCE_ID, OPTION_ID),
  };
}

function lockSeats(queueToken, seatCatalog) {
  const result = {
    lockedSeatIds: [],
    success: false,
  };
  const occupied = new Set();
  const sold = new Set();
  let lockSentAt = 0;
  let activeLockTargets = [];

  const response = ws.connect(buildSockJsWebSocketUrl(queueToken, PERFORMANCE_ID, OPTION_ID), {}, (socket) => {
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

      if (message.type === 'SEAT_LOCKED' && Array.isArray(message.seatIds)) {
        message.seatIds.forEach((seatId) => occupied.add(seatId));
        if (!result.success && activeLockTargets.length > 0 && activeLockTargets.every((seatId) => message.seatIds.includes(seatId))) {
          result.lockedSeatIds = [...activeLockTargets];
          result.success = true;
          seatSecured.add(1);
          seatResponseTime.add(Date.now() - lockSentAt);
          sockjsSend(socket, createStompFrame('SEND', {
            destination: '/app/seats/going-to-payment',
            'content-type': 'application/json',
          }, '{}'));
          socket.setTimeout(() => {
            sockjsSend(socket, createStompFrame('DISCONNECT'));
            socket.close();
          }, 150);
        }
      }
      if (message.type === 'SEAT_RELEASED' && Array.isArray(message.seatIds)) {
        message.seatIds.forEach((seatId) => occupied.delete(seatId));
      }
      if (message.type === 'SEAT_SOLD' && Array.isArray(message.seatIds)) {
        message.seatIds.forEach((seatId) => sold.add(seatId));
      }
      if (message.type === 'SEAT_EXPIRED' && Array.isArray(message.seatIds)) {
        message.seatIds.forEach((seatId) => occupied.delete(seatId));
      }

      if (message.type === 'INITIAL_STATE') {
        (message.occupiedSeats || []).forEach((seatId) => occupied.add(seatId));
        (message.soldSeats || []).forEach((seatId) => sold.add(seatId));
        const targets = pickSeatTargets(
          seatCatalog.seatIds,
          new Set([...occupied, ...sold]),
          SEATS_PER_ORDER
        );
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
        return;
      }

      if (message.type === 'LOCK_SUCCESS' && Array.isArray(message.seatIds)) {
        result.lockedSeatIds = message.seatIds;
        result.success = true;
        activeLockTargets = [];
        seatSecured.add(1);
        seatResponseTime.add(Date.now() - lockSentAt);
        sockjsSend(socket, createStompFrame('SEND', {
          destination: '/app/seats/going-to-payment',
          'content-type': 'application/json',
        }, '{}'));
        socket.setTimeout(() => {
          sockjsSend(socket, createStompFrame('DISCONNECT'));
          socket.close();
        }, 150);
        return;
      }

      if (message.type === 'LOCK_FAILED') {
        seatDenied.add(1);
        seatResponseTime.add(Date.now() - lockSentAt);
        socket.close();
      }
    });

    socket.setTimeout(() => socket.close(), WS_CONNECT_TIMEOUT_MS);
  });

  check(response, {
    'full flow websocket connected': (res) => res && res.status === 101,
  });
  return result;
}

function preparePayment(accessToken, queueToken, lockedSeatIds, seatCatalog) {
  const amount = lockedSeatIds.reduce((sum, seatId) => sum + (seatCatalog.byId[seatId]?.price || 0), 0);
  const payload = {
    tokenId: queueToken,
    performanceId: PERFORMANCE_ID,
    optionId: OPTION_ID,
    amount,
    seatIds: lockedSeatIds.map((seatId) => String(seatId)),
  };
  const startedAt = Date.now();
  const response = http.post(`${API_BASE_URL}/api/payments/prepare`, JSON.stringify(payload), {
    headers: authHeaders(accessToken, { 'Content-Type': 'application/json' }),
    timeout: '10s',
  });
  paymentPrepareTime.add(Date.now() - startedAt);
  if (response.status !== 200) {
    return null;
  }
  const body = response.json();
  return {
    orderId: body.orderId,
    amount: body.amount,
  };
}

function confirmPayment(accessToken, orderId, amount) {
  const startedAt = Date.now();
  const response = http.post(`${API_BASE_URL}/api/payments/confirm`, JSON.stringify({
    orderId,
    paymentKey: `${PAYMENT_KEY_PREFIX}-${__VU}-${__ITER}`,
    amount,
  }), {
    headers: authHeaders(accessToken, { 'Content-Type': 'application/json' }),
    timeout: '10s',
  });
  paymentConfirmTime.add(Date.now() - startedAt);
  if (response.status !== 200) {
    return null;
  }
  return response.json();
}

function waitForReservation(accessToken, expectedSeatPos) {
  const startedAt = Date.now();
  while (Date.now() - startedAt < RESERVATION_POLL_MAX_MS) {
    const response = http.get(`${API_BASE_URL}/api/reservations/my?type=ALL&size=20`, {
      headers: authHeaders(accessToken),
      timeout: '5s',
    });
    if (response.status === 200) {
      const body = response.json();
      const content = body.content || [];
      const seatSet = new Set(content
        .filter((item) => item.performanceOptionId === OPTION_ID && item.reservationStatus === 'ISSUED')
        .map((item) => item.seatPos));
      const visible = expectedSeatPos.every((seatPos) => seatSet.has(seatPos));
      if (visible) {
        reservationProjectionDelay.add(Date.now() - startedAt);
        return true;
      }
    }
    sleep(RESERVATION_POLL_MS / 1000);
  }
  return false;
}

export default function (data) {
  const userId = data.userIds[(__VU - 1) % data.userIds.length];
  const accessToken = login(userId, metrics);
  if (!accessToken) {
    fullFlowSuccess.add(false);
    return;
  }

  const queueToken = joinQueueAndGetToken(accessToken, PERFORMANCE_ID, OPTION_ID, metrics);
  if (!queueToken) {
    fullFlowSuccess.add(false);
    return;
  }

  const seatResult = lockSeats(queueToken, data.seatCatalog);
  if (!seatResult.success || seatResult.lockedSeatIds.length === 0) {
    fullFlowSuccess.add(false);
    return;
  }

  const prepareResult = preparePayment(accessToken, queueToken, seatResult.lockedSeatIds, data.seatCatalog);
  if (!prepareResult) {
    fullFlowSuccess.add(false);
    return;
  }

  const confirmResult = confirmPayment(accessToken, prepareResult.orderId, prepareResult.amount);
  if (!confirmResult || confirmResult.status !== 'SUCCESS') {
    fullFlowSuccess.add(false);
    return;
  }

  const expectedSeatPos = seatResult.lockedSeatIds.map((seatId) => data.seatCatalog.byId[seatId].seatPos);
  fullFlowSuccess.add(waitForReservation(accessToken, expectedSeatPos));
}

export function handleSummary(data) {
  return {
    stdout: [
      '',
      '=== Monolith Full Flow Benchmark ===',
      `login_success=${data.metrics.login_success?.values.count || 0}`,
      `queue_joined=${data.metrics.queue_joined?.values.count || 0}`,
      `token_issued=${data.metrics.token_issued?.values.count || 0}`,
      `seat_secured=${data.metrics.seat_secured?.values.count || 0}`,
      `payment_confirm_p95=${data.metrics.payment_confirm_ms?.values['p(95)'] || 0}`,
      `reservation_projection_p95=${data.metrics.reservation_projection_delay_ms?.values['p(95)'] || 0}`,
      '',
    ].join('\n'),
    'k6/results/full-flow-result.json': JSON.stringify(data, null, 2),
  };
}
