# DDIB 현재 아키텍처 분석

## 범위
- 분석 대상: `/Users/yewonchoi/Desktop/DDIB`
- 목적: 현재 MSA 구조를 이해하고, 이후 동일한 예매 플로우를 비교할 수 있는 모놀리식 마이그레이션의 출발점을 정의
- 기준: 실행 결과가 아니라 코드 구조, 설정 파일, 프론트 API 호출부, k6 시나리오 기준 정적 분석

## 1. 서비스 목록과 책임

| 서비스 | 기본 포트 | 현재 책임 | 주요 저장소/인프라 |
| --- | --- | --- | --- |
| Front | 5173 | 사용자 UI, 로그인, 공연 조회, 대기열 진입, 좌석 WebSocket, 결제, 내 예매 조회 | Gateway 경유 HTTP, RealTime WebSocket |
| Gateway | 8080 | 외부 단일 진입점, 라우팅, JWT 검증 후 `X-User-*` 헤더 주입, Swagger 병합 | 자체 상태 저장 없음 |
| Auth | 8081 | 소셜 로그인, JWT 발급/재발급, 사용자 조회/수정/탈퇴 | MySQL(auth_db), Redis(refresh/blacklist) |
| Ticketing | 8082 | 공연/회차/좌석 메타데이터, 좌석 캐시, 티켓 발급/취소/교환, 후속 이벤트 발행 | MySQL(ticketing_db), Redis(seat cache/state), Kafka(outbox producer/consumer) |
| Swap | 8083 | 티켓 교환 게시 상태, 교환 요청 생성/수락/거절/취소, 교환 알림 | MySQL(swap_db), Kafka consumer, Ticketing 내부 API 호출 |
| Payment | 8084 | 결제 준비/승인/취소, PG 연동, 결제 상태 관리, 결제 이벤트 발행 | MySQL(payment_db), Redis(queue token 검증), Kafka(outbox producer/consumer) |
| Queue | 8085 | 대기열 등록, 순번 조회 SSE, 활성 토큰 발급/만료, 재진입 제어 | Redis(waiting ZSET, active token, user token) |
| Reservation | 8086 | 내 예매/티켓 조회용 read model, 티켓 발급/교환/환불 이벤트 반영 | MySQL(reservation), Kafka consumer |
| RealTime | 8087 | 좌석 점유/해제 WebSocket, 실시간 브로드캐스트, queue token 만료에 따른 좌석 회수 | Redis(lock, occupied, sold, pub/sub) |

## 2. 각 서비스의 주요 API

### Front
- 공연 목록/상세/좌석: `/api/ticketing/performances/**`
- 대기열: `/api/queue/in`, `/api/queue/status`
- 결제: `/api/payments/prepare`, `/api/payments/confirm`
- 내 예매: `/api/reservations/my`, `/api/reservations/my/{reservationId}`
- 좌석 실시간 연결: `/ws?queueToken=...&performanceId=...&optionId=...`

### Gateway
- 라우팅 prefix
- `/api/auth/**`, `/api/users/**`
- `/api/ticketing/**`
- `/api/swap/**`
- `/api/payments/**`
- `/api/queue/**`
- `/api/reservations/**`
- `/api/realtime/**`, `/app/**`, `/ws/**`

### Auth
- `POST /api/auth/login/{provider}`
- `GET /api/auth/login/{provider}/url`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/users/me`
- `PATCH /api/users/me`
- `DELETE /api/users/me`

### Ticketing
- `POST /api/ticketing/performances`
- `GET /api/ticketing/performances`
- `GET /api/ticketing/performances/{performanceId}`
- `GET /api/ticketing/performances/{performanceId}/options/{optionId}/seats`
- `GET /api/ticketing/tickets/me`
- `GET /api/ticketing/tickets/me/{ticketId}`
- `GET /api/ticketing/tickets/me/by-performance-status`
- 내부 API
- `GET /internal/ticketing/performances/{performanceOptionId}/seats`
- `GET /internal/ticketing/tickets/{ticketId}/seat-snapshot`
- `POST /internal/ticketing/swap`

### Swap
- `POST /api/swap/{ticketId}/{status}`
- `GET /api/swap/{ticketId}/status`
- `GET /api/swap/{swapId}/seats`
- `POST /api/swap/request`
- `GET /api/swap/request/received`
- `GET /api/swap/request/received/{swapId}`
- `GET /api/swap/request/sent`
- `PATCH /api/swap/request/{swapId}/accept`
- `PATCH /api/swap/request/{swapId}/reject`
- `PATCH /api/swap/request/{swapId}/cancel`
- `GET /api/swap/notice`

### Payment
- `POST /api/payments/prepare`
- `POST /api/payments/confirm`
- `POST /api/payments/{paymentId}/cancel`
- `GET /api/payments/{orderId}`
- `GET /api/payments/my`

### Queue
- `POST /api/queue/in`
- `GET /api/queue/status` with `text/event-stream`

### Reservation
- `GET /api/reservations/my`
- `GET /api/reservations/my/{reservationId}`
- `POST /api/reservations/dummy`

### RealTime
- 연결 endpoint: `/ws?queueToken=...&performanceId=...&optionId=...`
- STOMP message mappings
- `/app/seats/init`
- `/app/seats/lock`
- `/app/seats/release`
- `/app/seats/going-to-payment`
- `/app/token/invalidate`
- 구독 channel
- `/topic/seats.{performanceId}.{optionId}`
- `/user/queue/seats`

## 3. 서비스 간 호출 관계

### 동기 HTTP 호출
- Front -> Gateway -> Auth/Ticketing/Queue/Payment/Reservation/Swap/RealTime
- Gateway -> 각 백엔드 서비스
- Swap -> Ticketing 내부 API
  - 좌석 스냅샷 조회
  - 공연 회차 좌석 스냅샷 조회
  - 실제 교환 실행
- Auth -> Kakao/Google 외부 API
- Payment -> Toss Payments 외부 API

### 비동기 Kafka 호출
- Payment -> `payment.succeeded` -> Ticketing
- Payment -> `payment.canceled` -> Ticketing
- Payment -> `payment.failed`, `payment.invalidated`, `payment.expired`
  - 현재 코드상 주요 후속 소비자는 예매 핵심 플로우보다 운영/확장용에 가까움
- Ticketing -> `ticket.issued` -> Reservation
- Ticketing -> `reservation.ticket-swapped` -> Reservation
- Ticketing -> `reservation.ticket-refunded` -> Reservation
- Ticketing -> `swap.ticket-refunded` -> Swap
- Ticketing -> `ticket.swapped` -> Payment

### Redis 기반 느슨한 결합
- Queue가 발급한 queue token을 RealTime과 Payment가 직접 Redis에서 검증
- Ticketing이 결제 성공/취소 시 좌석 상태와 sold set을 Redis에 반영
- Ticketing이 `seat:broadcast` pub/sub를 발행하고 RealTime이 수신해 WebSocket으로 재브로드캐스트

### 호출 구조 요약

```text
Client
  -> Gateway
    -> Auth
    -> Ticketing
    -> Queue
    -> Payment
    -> Reservation
    -> Swap
    -> RealTime

Queue --Redis token--> RealTime
Queue --Redis token--> Payment
Ticketing --Redis seat state/pubsub--> RealTime
Payment --Kafka payment.succeeded/canceled--> Ticketing
Ticketing --Kafka ticket.issued/refunded/swapped--> Reservation / Swap / Payment
Swap --HTTP internal--> Ticketing
```

## 4. DB/Redis/Kafka 사용 지점

| 서비스 | MySQL | Redis | Kafka |
| --- | --- | --- | --- |
| Auth | `User`, `Credential` 저장 | refresh token, blacklist token 저장 | 없음 |
| Gateway | 없음 | 없음 | 없음 |
| Ticketing | `Performance`, `PerformanceOption`, `Venue`, `Seat`, `Ticket`, `TicketHistory`, `TicketSwapRequestLog`, `Outbox` | 좌석 메타 캐시 `option:{id}:seats:info`, sold set, occupied set, seat lock key, user queue token 삭제, `seat:broadcast` 발행 | 결제 성공/취소 소비, 티켓 발급/환불/교환 이벤트 outbox 발행 |
| Swap | `Swap`, `SwapRequest`, `ProcessedEvent` | 핵심 로직에서는 실사용 흔적 없음 | `swap.ticket-refunded` 소비 |
| Payment | `Payment`, `Outbox`, `ProcessedEvent` | `option:{optionId}:queueToken:{token}` TTL/값 조회로 queue token 검증 | `ticket.swapped` 소비, 결제 상태 이벤트 outbox 발행 |
| Queue | 없음 | waiting queue ZSET, active token key, user->token 역인덱스, 만료/삭제 이벤트 기반 보충 발급 | 없음 |
| Reservation | `Reservation`, `ProcessedEvent` | 설정은 있으나 핵심 도메인 코드에서 실사용 거의 없음 | `ticket.issued`, `reservation.ticket-swapped`, `reservation.ticket-refunded` 소비 |
| RealTime | 없음 | 좌석 lock, occupied/sold set, user queue token TTL 조회, key expiration listener, `seat:broadcast` 구독 | 없음 |

### Redis 키 관점의 핵심 공유 상태
- `queue:waiting:option:{optionId}`: Queue 대기열 ZSET
- `option:{optionId}:queueToken:{queueToken}`: 활성 진입 토큰
- `user:{userId}:option:{optionId}:queueToken`: 사용자별 토큰 역인덱스
- `option:{optionId}:seat:lock:{seatId}`: 좌석 잠금
- `option:{optionId}:occupied:seats`: 현재 점유 좌석
- `option:{optionId}:sold:seats`: 판매 완료 좌석
- `seat:broadcast`: Ticketing -> RealTime 실시간 이벤트 채널

## 5. 핵심 예매 플로우

1. 사용자가 Front에서 공연 목록/상세/좌석 메타데이터를 조회한다.
2. 로그인된 사용자가 Queue에 `POST /api/queue/in`으로 진입한다.
3. Queue는 Redis waiting ZSET에 사용자를 넣고, 스케줄러가 활성 슬롯이 비면 queue token을 발급한다.
4. Front는 SSE `/api/queue/status`를 구독하다가 `ISSUED`와 queue token을 수신한다.
5. Front는 queue token으로 RealTime WebSocket `/ws?...`에 접속한다.
6. RealTime은 Redis에 있는 queue token을 검증하고, 좌석 lock/해제를 Redis Lua 스크립트로 처리한다.
7. 사용자가 결제 페이지로 이동하면 RealTime에 `/app/seats/going-to-payment`를 보내 disconnect 중에도 좌석을 유지한다.
8. Front는 Payment `POST /api/payments/prepare`를 호출한다.
9. Payment는 Redis의 queue token TTL과 사용자 일치 여부를 검증하고 READY 결제를 만든다.
10. Front는 Payment `POST /api/payments/confirm`을 호출한다.
11. Payment는 PG 승인 후 `payment.succeeded` 이벤트를 Kafka로 발행한다.
12. Ticketing은 `payment.succeeded`를 소비해 티켓을 발급하고, sold seat 반영 및 `seat:broadcast` 발행, queue token 정리, `ticket.issued` 이벤트를 발행한다.
13. Reservation은 `ticket.issued`를 소비해 내 예매 조회용 read model을 만든다.
14. Front의 내 티켓/예매 화면은 Reservation API를 조회한다.

### 현재 구조의 중요한 특징
- 동기식 핵심 경로는 `Client -> Queue/RealTime/Payment`까지 이어지고, 결제 성공 이후 정합성 반영은 Kafka 기반 후행 처리다.
- 예매 성공 직후 사용자용 조회 모델은 Ticketing이 아니라 Reservation이 담당한다.
- Queue, RealTime, Ticketing, Payment는 서로 직접 API 호출보다 Redis 공유 상태에 더 강하게 결합되어 있다.

## 6. 모놀리식으로 합칠 때의 후보 패키지 구조

현재 서비스 경계를 그대로 패키지 경계로 옮기되, 비교 실험을 위해 `queue`와 `seat-realtime`을 유지하는 구성이 가장 안전하다.

```text
com.ddib.monolith
  auth
    api
    application
    domain
    infra
  performance
    api
    application
    domain
    infra
  queue
    api
    application
    domain
    infra
  seat
    websocket
    application
    domain
    infra
  payment
    api
    application
    domain
    infra
  ticket
    api
    application
    domain
    infra
  reservation
    api
    application
    domain
    infra
  swap
    api
    application
    domain
    infra
  support
    security
    web
    redis
    messaging
    outbox
    tracing
```

### 패키지 합치기 원칙
- `Gateway`는 별도 서비스가 아니라 monolith 내부 filter/config로 흡수
- `Ticketing`, `Reservation`, `Swap`, `Payment`는 도메인별 패키지로 유지
- `Queue`와 `RealTime`은 성능 비교 핵심이라 독립 패키지로 유지
- Kafka는 1차적으로 `support.messaging` 아래 추상화하고, 모놀리식 MVP에서는 동일 JVM 내부 이벤트로 대체 가능
- Redis 키 구조는 초기에 그대로 유지하는 편이 비교 실험에 유리

## 7. 성능 비교 시나리오 후보

| 시나리오 | 현재 코드 근거 | 비교 포인트 |
| --- | --- | --- |
| 공연 오픈 직후 대기열 진입 폭주 | `k6/full-flow-test.js`, `k6/full-flow-test2.js` | queue join TPS, token 발급 지연, Redis 부하 |
| 좌석 선점 경쟁 | `k6/realtime-stress-test.js` | WebSocket 연결 수, lock 성공률, lock p95, 좌석 경합 시 race lost 비율 |
| 전체 예매 플로우 | `k6/full-flow-test.js` | queue 진입부터 결제 성공까지 end-to-end latency |
| 새로고침/이탈/재진입이 많은 대기열 | `k6/queue-refresh-test.js` | token 재발급 안정성, 만료 후 좌석 회수, orphaned lock 발생 여부 |
| 결제 성공 후 후행 정합성 반영 | Payment -> Ticketing -> Reservation 이벤트 체인 | payment success 후 ticket visible까지의 지연 |
| 취소/환불 플로우 | Payment canceled -> Ticketing -> Reservation/Swap | 좌석 복원 지연, sold/occupied 정합성 |
| 티켓 교환 플로우 | Swap -> Ticketing internal HTTP -> Payment/Reservation Kafka | 비관적 락 경쟁, 교환 완료 지연 |

### 비교 지표 제안
- 응답 시간: p50, p95, p99
- 처리량: queue join RPS, payment confirm TPS
- 성공률: seat lock success rate, payment success rate
- 후행 정합성 지연: payment success 시점부터 reservation 조회 가능 시점까지
- 인프라 지표: Redis ops/sec, Kafka lag, DB connection usage

## 8. 가장 작게 시작할 수 있는 MVP 범위

### 추천 MVP
- 포함
- 고정 사용자 또는 단순 JWT 기반 인증
- 공연 목록/상세/좌석 메타 조회
- 대기열 등록/토큰 발급
- 실시간 좌석 lock/release
- 결제 prepare/confirm
- 티켓 발급
- 내 예매 조회용 read model 생성
- 제외
- 소셜 로그인(Kakao/Google)
- 티켓 교환(Swap)
- 결제 취소/환불
- 운영용 Swagger 병합/분산 tracing
- 다중 서비스 배포/게이트웨이 분리

### 이유
- 이 범위만으로도 사용자가 체감하는 핵심 예매 플로우를 완결할 수 있다.
- 동시에 현재 MSA의 병목인 `Queue -> RealTime -> Payment -> Ticketing -> Reservation` 경로를 그대로 비교 대상으로 유지할 수 있다.
- Swap과 취소/환불은 중요하지만, 1차 비교 실험 없이도 확장 가능한 후속 범위다.

## 결론

현재 DDIB는 서비스가 많이 쪼개져 있지만, 예매 핵심 경로만 보면 실질적으로는 다음 네 축에 의해 움직인다.

1. `Queue + Redis token`
2. `RealTime + Redis seat lock`
3. `Payment + Redis token validation + Kafka publish`
4. `Ticketing + Reservation`의 티켓 발급/조회 반영

모놀리식 마이그레이션의 1차 목표는 이 네 축을 하나의 코드베이스 안으로 가져오되, Redis 기반 좌석/대기열 모델은 초기에 유지하는 것이다. 그렇게 해야 "서비스 간 네트워크 홉 제거" 효과와 "도메인 로직 자체의 병목"을 분리해서 비교할 수 있다.
