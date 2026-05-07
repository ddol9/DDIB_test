import http from 'k6/http';
import { sleep } from 'k6';

export const API_BASE_URL = (__ENV.API_BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/, '');
export const WS_BASE_URL = (__ENV.WS_BASE_URL || API_BASE_URL.replace(/^http/, 'ws') + '/ws').replace(/\/$/, '');
export const PERFORMANCE_ID = __ENV.PERFORMANCE_ID ? parseInt(__ENV.PERFORMANCE_ID, 10) : 1;
export const OPTION_ID = __ENV.OPTION_ID ? parseInt(__ENV.OPTION_ID, 10) : 1;
export const MAX_VUS = __ENV.MAX_VUS ? parseInt(__ENV.MAX_VUS, 10) : 20;
export const WAIT_POLL_MS = __ENV.WAIT_POLL_MS ? parseInt(__ENV.WAIT_POLL_MS, 10) : 1000;
export const WAIT_MAX_MS = __ENV.WAIT_MAX_MS ? parseInt(__ENV.WAIT_MAX_MS, 10) : 30000;
export const WS_CONNECT_TIMEOUT_MS = __ENV.WS_CONNECT_TIMEOUT_MS ? parseInt(__ENV.WS_CONNECT_TIMEOUT_MS, 10) : 12000;
export const SEATS_PER_ORDER = __ENV.SEATS_PER_ORDER ? parseInt(__ENV.SEATS_PER_ORDER, 10) : 2;

export function randomString(length) {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < length; i += 1) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

export function sockjsSend(socket, data) {
  socket.send(JSON.stringify([data]));
}

export function createStompFrame(command, headers = {}, body = '') {
  let frame = `${command}\n`;
  Object.entries(headers).forEach(([key, value]) => {
    frame += `${key}:${value}\n`;
  });
  frame += `\n${body}\0`;
  return frame;
}

export function parseStompFrame(data) {
  let content = data;
  if (typeof data === 'string' && data.startsWith('a[')) {
    try {
      const arr = JSON.parse(data.substring(1));
      content = arr[0] || '';
    } catch (error) {
      content = data;
    }
  }
  const nullIndex = content.indexOf('\0');
  const frameContent = nullIndex >= 0 ? content.substring(0, nullIndex) : content;
  const lines = frameContent.split('\n');
  const command = lines[0];
  const headers = {};
  let bodyStartIndex = 1;
  for (let i = 1; i < lines.length; i += 1) {
    if (lines[i] === '') {
      bodyStartIndex = i + 1;
      break;
    }
    const colonIndex = lines[i].indexOf(':');
    if (colonIndex >= 0) {
      headers[lines[i].substring(0, colonIndex)] = lines[i].substring(colonIndex + 1);
    }
  }
  return {
    command,
    headers,
    body: lines.slice(bodyStartIndex).join('\n'),
  };
}

export function buildSockJsWebSocketUrl(queueToken, performanceId = PERFORMANCE_ID, optionId = OPTION_ID) {
  const serverId = Math.floor(Math.random() * 1000);
  const sessionId = randomString(8);
  return `${WS_BASE_URL}/${serverId}/${sessionId}/websocket?performanceId=${performanceId}&optionId=${optionId}&queueToken=${encodeURIComponent(queueToken)}`;
}

export function authHeaders(accessToken, extra = {}) {
  return {
    Authorization: `Bearer ${accessToken}`,
    ...extra,
  };
}

export function extractQueueStatus(body) {
  if (!body) {
    return null;
  }
  const lines = body.split('\n');
  for (let i = lines.length - 1; i >= 0; i -= 1) {
    if (!lines[i].startsWith('data:')) {
      continue;
    }
    try {
      const parsed = JSON.parse(lines[i].substring(5).trim());
      if (parsed.status) {
        return parsed;
      }
    } catch (error) {
      // ignore invalid SSE fragment
    }
  }

  const tokenMatch = body.match(/"queueToken"\s*:\s*"([^"]+)"/);
  const statusMatch = body.match(/"status"\s*:\s*"([A-Z_]+)"/);
  const rankMatch = body.match(/"rank"\s*:\s*(-?\d+)/);
  if (!statusMatch) {
    return null;
  }
  return {
    status: statusMatch[1],
    queueToken: tokenMatch ? tokenMatch[1] : null,
    rank: rankMatch ? parseInt(rankMatch[1], 10) : null,
  };
}

export function seedUsers(requiredCount) {
  const userIds = [1];
  while (userIds.length < requiredCount) {
    const response = http.post(`${API_BASE_URL}/api/auth/test/dummy-users`);
    if (response.status !== 200) {
      throw new Error(`Failed to seed benchmark users: status=${response.status}`);
    }
    const created = response.json();
    if (Array.isArray(created)) {
      created.forEach((userId) => {
        if (!userIds.includes(userId)) {
          userIds.push(userId);
        }
      });
    }
  }
  return userIds.slice(0, requiredCount);
}

export function login(userId, metrics) {
  const startedAt = Date.now();
  const response = http.post(`${API_BASE_URL}/api/auth/test/login?userId=${userId}`);
  metrics?.loginTime?.add(Date.now() - startedAt);
  if (response.status !== 200) {
    metrics?.loginFailed?.add(1);
    return null;
  }
  metrics?.loginSuccess?.add(1);
  try {
    const body = response.json();
    return body.accessToken || null;
  } catch (error) {
    metrics?.loginFailed?.add(1);
    return null;
  }
}

export function joinQueueAndGetToken(accessToken, performanceId, optionId, metrics) {
  const queueStartedAt = Date.now();
  const joinResponse = http.post(
    `${API_BASE_URL}/api/queue/in`,
    JSON.stringify({ performanceId, optionId }),
    {
      headers: authHeaders(accessToken, { 'Content-Type': 'application/json' }),
      timeout: '10s',
    }
  );
  if (joinResponse.status !== 200) {
    metrics?.queueJoinFailed?.add(1);
    return null;
  }
  metrics?.queueJoined?.add(1);

  while (Date.now() - queueStartedAt < WAIT_MAX_MS) {
    const statusResponse = http.get(
      `${API_BASE_URL}/api/queue/status?performanceId=${performanceId}&optionId=${optionId}`,
      {
        headers: authHeaders(accessToken, { Accept: 'text/event-stream' }),
        timeout: '5s',
      }
    );
    const parsed = extractQueueStatus(statusResponse.body || '');
    if (parsed && parsed.status === 'ISSUED' && parsed.queueToken) {
      metrics?.tokenIssued?.add(1);
      metrics?.queueToTokenTime?.add(Date.now() - queueStartedAt);
      return parsed.queueToken;
    }
    if (parsed && parsed.status === 'REJECT') {
      metrics?.tokenFailed?.add(1);
      return null;
    }
    sleep(WAIT_POLL_MS / 1000);
  }

  metrics?.tokenFailed?.add(1);
  return null;
}

export function fetchSeatCatalog(performanceId = PERFORMANCE_ID, optionId = OPTION_ID) {
  const response = http.get(`${API_BASE_URL}/api/ticketing/performances/${performanceId}/options/${optionId}/seats`, {
    timeout: '10s',
  });
  if (response.status !== 200) {
    throw new Error(`Failed to fetch seat catalog: status=${response.status}`);
  }
  const body = response.json();
  const rows = {};
  (body.seatTypes || []).forEach((seatType) => {
    (seatType.rows || []).forEach((rowLabel) => {
      rows[rowLabel] = {
        seatType: seatType.seatType,
        price: seatType.price,
      };
    });
  });

  const byId = {};
  const seatIds = [];
  (body.seats || []).forEach((seat) => {
    seatIds.push(seat.id);
    byId[seat.id] = {
      id: seat.id,
      row: seat.label,
      number: seat.number,
      seatType: rows[seat.label]?.seatType || seat.label,
      price: rows[seat.label]?.price || 0,
      seatPos: `${seat.label}-${seat.number}`,
    };
  });
  return { seatIds, byId };
}

export function pickSeatTargets(allSeatIds, unavailable, count) {
  const available = allSeatIds.filter((seatId) => !unavailable.has(seatId));
  for (let i = available.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1));
    [available[i], available[j]] = [available[j], available[i]];
  }
  return available.slice(0, Math.min(count, available.length));
}
