# Monolith Benchmark Guide

## 목적
- monolith와 기존 MSA를 같은 예매 플로우로 비교 가능한 상태로 맞춘다.
- 비교 단위는 `queue -> seat lock -> payment confirm -> reservation visible`이다.

## 전제
- monolith는 `local` 프로필로 실행한다.
- `AuthSeedDataInitializer`, `PerformanceSeedDataInitializer`가 활성화되어 있어야 한다.
- k6는 로컬 또는 별도 부하 생성기에서 실행 가능해야 한다.

## 현재 범위
- `full-flow-test.js`
- `realtime-stress-test.js`
- `queue-refresh-test.js`

주의:
- 현재 monolith MVP에는 "active token refresh" 로직이 없다.
- 따라서 `queue-refresh-test.js`는 이름은 유지하지만 실제로는 "queue re-entry + short hold churn" 시나리오로 동작한다.

## 실행 순서

### 1. monolith 기동

```bash
cd /Users/yewonchoi/Desktop/DDIB_test
./gradlew bootRun
```

기본 주소:
- HTTP: `http://127.0.0.1:8080`
- WebSocket: `ws://127.0.0.1:8080/ws`

### 2. 환경 변수 로드

```bash
cd /Users/yewonchoi/Desktop/DDIB_test
export $(grep -v '^#' k6/monolith.env.example | xargs)
```

필요 시 개별 override:

```bash
export MAX_VUS=50
export PERFORMANCE_ID=1
export OPTION_ID=1
```

### 3. 부하 테스트 실행

전체 플로우:

```bash
k6 run k6/full-flow-test.js
```

실시간 경쟁:

```bash
k6 run k6/realtime-stress-test.js
```

대기열 재진입 churn:

```bash
k6 run k6/queue-refresh-test.js
```

## 필수 seed 데이터

### 사용자
- k6 `setup()` 단계에서 `/api/auth/test/dummy-users`를 호출해 필요한 수만큼 테스트 사용자를 만든다.
- 기본 seed user `1`도 포함한다.

### 공연/좌석
- 현재 seed performance는 `performanceId=1`, `optionId=1`을 기준으로 준비되어 있다.
- 좌석 카탈로그는 `/api/ticketing/performances/{performanceId}/options/{optionId}/seats`에서 읽는다.

### 좌석 가격
- `full-flow-test.js`는 좌석 정보 API 응답의 `seatTypes`와 `seats`를 조합해 좌석별 금액을 계산한다.
- 따라서 `PaymentPrepareService`가 기대하는 금액과 k6 요청 금액이 동일하게 유지된다.

## 환경 변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `API_BASE_URL` | `http://127.0.0.1:8080` | monolith HTTP base URL |
| `WS_BASE_URL` | `ws://127.0.0.1:8080/ws` | SockJS base URL |
| `PERFORMANCE_ID` | `1` | 대상 공연 ID |
| `OPTION_ID` | `1` | 대상 회차 ID |
| `MAX_VUS` | `20` | 기본 동시 사용자 수 |
| `WAIT_POLL_MS` | `1000` | queue status polling 간격 |
| `WAIT_MAX_MS` | `30000` | queue token 최대 대기 시간 |
| `WS_CONNECT_TIMEOUT_MS` | `12000` | WebSocket 세션 강제 종료 시간 |
| `SEATS_PER_ORDER` | `2` | 주문당 좌석 수 |
| `PAYMENT_KEY_PREFIX` | `benchmark-payment` | 결제 승인용 paymentKey prefix |
| `RESERVATION_POLL_MS` | `250` | reservation visible polling 간격 |
| `RESERVATION_POLL_MAX_MS` | `5000` | reservation visible 최대 대기 시간 |
| `FULL_FLOW_STAGE_1~4` | `10s/10s/40s/10s` | full-flow ramp/hold duration |
| `QUEUE_REFRESH_STAGE_1~3` | `10s/40s/10s` | queue-refresh ramp/hold duration |
| `REENTRY_COUNT` | `3` | queue-refresh 재진입 횟수 |
| `ACTIONS_PER_SESSION` | `4` | realtime-stress 한 세션당 lock/release 횟수 |

## 수집 지표

### 공통
- `login_time_ms`
- `queue_to_token_ms`

### 실시간 경쟁
- `seat_response_ms`
- `seat_secure_rate`
- `seat_secured`
- `seat_denied`
- `ws_connect_failed`

### 전체 플로우
- `payment_prepare_ms`
- `payment_confirm_ms`
- `reservation_projection_delay_ms`
- `full_flow_success_rate`

## MSA와 맞출 비교 조건

1. 같은 `performanceId`, `optionId`를 사용한다.
2. 같은 `SEATS_PER_ORDER`를 사용한다.
3. 같은 `MAX_VUS`, stage duration, polling 간격을 사용한다.
4. 가능하면 동일한 k6 실행 호스트에서 두 시스템을 번갈아 실행한다.
5. 결과 비교 시 다음 지표를 최소 포함한다.
   - `queue_to_token_ms p95`
   - `seat_response_ms p95`
   - `payment_confirm_ms p95`
   - `reservation_projection_delay_ms p95`

## 해석 포인트

### monolith가 유리할 수 있는 구간
- service-to-service network hop 제거
- Kafka broker round-trip 제거
- payment success 이후 reservation visible까지 동기 처리

### monolith가 불리할 수 있는 구간
- 단일 애플리케이션 내부에서 queue, websocket, payment 요청이 같은 프로세스 자원을 공유
- JVM 한 인스턴스에 부하가 몰리면 seat lock latency tail이 나빠질 수 있음

## 현재 한계
- `payment cancel`, refund propagation, swap은 아직 benchmark 대상에 넣지 않았다.
- `queue-refresh-test.js`는 원본 MSA의 토큰 갱신 의미를 완전히 복원하지 않는다.
- 현재 reservation projection은 내부 이벤트 기반 동기 처리라 Kafka 기반 eventual consistency와 직접 비교 시 해석 주의가 필요하다.
