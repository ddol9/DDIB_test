# DDIB 모놀리식 패키지 설계

## 문서 목적
- 모놀리식 전환 시 사용할 코드 구조를 미리 고정한다.
- 서비스가 사라진 뒤에도 도메인 경계가 흐려지지 않도록 패키지와 책임을 명확히 한다.

## 설계 원칙

### 1. 서비스명 대신 도메인명 유지
- 기존 서비스 분리는 사라지지만, `auth`, `queue`, `seat`, `payment`, `ticket`, `reservation`, `swap` 경계는 유지한다.

### 2. 계층은 얇고 명확하게
- `api`: controller, request/response
- `application`: use case orchestration
- `domain`: entity, enum, domain service, repository port
- `infra`: JPA/Redis/Kafka adapter, 외부 연동 구현

### 3. 공통은 support로 모으되 남용 금지
- 진짜 공통만 `support`에 둔다.
- 특정 도메인 전용 유틸은 해당 도메인에 남긴다.

### 4. 비교 실험에 필요한 경계 유지
- queue와 seat는 성능 실험의 중심이므로 독립 패키지로 유지한다.
- reservation은 read model 성격이 강하므로 ticket과 분리 유지한다.

## 최상위 패키지 제안

```text
com.ddib.monolith
  auth
  performance
  queue
  seat
  payment
  ticket
  reservation
  swap
  support
```

## 전체 구조 예시

```text
com.ddib.monolith
  MonolithApplication

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
    api
    application
    domain
    infra
    websocket

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
    config
    security
    web
    exception
    logging
    event
    outbox
    persistence
    redis
```

## 모듈별 책임

## auth
- 로그인
- 토큰 발급/재발급
- 사용자 조회/수정/탈퇴
- refresh token 저장

### auth 내부 예시

```text
auth
  api
    AuthController
    UserController
    dto
  application
    AuthFacade
    UserFacade
  domain
    model
      User
      Credential
    service
      AuthDomainService
    repository
      UserRepository
      CredentialRepository
  infra
    jwt
    oauth
    persistence
    redis
```

## performance
- 공연/회차 조회
- 좌석 메타 조회
- 좌석 메타 Redis 캐시

## queue
- 대기열 등록
- 순번 조회
- token 발급
- 스케줄러

### queue 내부 예시

```text
queue
  api
    QueueController
    dto
  application
    QueueFacade
    QueueScheduler
  domain
    service
      QueueDomainService
    model
      QueueToken
      QueueStatus
    repository
      QueueStore
  infra
    redis
      RedisQueueStore
    sse
```

## seat
- WebSocket/STOMP
- queue token 검증
- 좌석 점유/해제
- token expiration 처리
- 좌석 broadcast

### seat 내부 예시

```text
seat
  websocket
    SeatWebSocketController
    WebSocketConfig
    interceptor
    room
  application
    SeatFacade
    BroadcastFacade
  domain
    service
      SeatLockService
      TokenValidationService
      SeatEventService
    repository
      SeatStore
    model
      SeatLockResult
      SeatReleaseResult
  infra
    redis
      RedisSeatStore
      RedisEventPublisher
      RedisEventSubscriber
      SeatLockExpirationListener
```

## payment
- 결제 준비
- 결제 승인
- 결제 취소
- PG 연동
- payment event 발행

### payment 내부 예시

```text
payment
  api
    PaymentController
    dto
  application
    PaymentPrepareUseCase
    PaymentConfirmUseCase
    PaymentCancelUseCase
  domain
    model
      Payment
      PaymentStatus
    service
      PaymentTokenValidator
      PaymentAmountCalculator
    repository
      PaymentRepository
    event
      PaymentSucceededEvent
      PaymentCanceledEvent
  infra
    persistence
    pg
    event
```

## ticket
- 티켓 발급
- 티켓 취소/환불 반영
- 티켓 교환
- ticket history
- 좌석 sold 상태 반영

### ticket 내부 예시

```text
ticket
  api
    TicketController
    InternalSwapController
  application
    TicketIssueUseCase
    TicketCancelUseCase
    TicketSwapUseCase
    PaymentSucceededHandler
    PaymentCanceledHandler
  domain
    model
      Ticket
      TicketStatus
      TicketHistory
    service
      TicketIssueService
      TicketCancelService
      TicketSwapService
    repository
      TicketRepository
      TicketHistoryRepository
  infra
    persistence
    redis
```

## reservation
- 사용자 조회용 예매 read model
- ticket issued/swapped/refunded 후행 반영

### reservation 내부 예시

```text
reservation
  api
    ReservationController
  application
    ReservationQueryUseCase
    ReservationProjectionHandler
  domain
    model
      Reservation
      ReservationStatus
    repository
      ReservationRepository
  infra
    persistence
```

## swap
- 교환 게시 상태 관리
- 교환 요청 생성/수락/거절/취소
- 교환 알림

### 현재 판단
- MVP 범위에서는 패키지만 준비하고 실제 이식은 후순위
- 이유: Ticketing/Payment/Reservation 경로보다 실험 핵심성이 낮고, 락/정합성 로직이 복잡함

## support 패키지 구성

### support.config
- 공통 `application` 설정
- Jackson
- JPA
- Redis
- WebMvc/WebFlux
- WebSocket 공통 설정

### support.security
- JWT provider
- 인증 필터/interceptor
- 인증 context
- `@UserId` 류 argument resolver

### support.web
- 공통 API 응답
- CORS
- request logging

### support.exception
- 공통 error code
- global exception handler

### support.event
- `DomainEventPublisher`
- 내부 이벤트 구현
- 이벤트 핸들러 공통 규약

### support.outbox
- outbox entity
- outbox repository
- outbox publisher interface
- 필요 시 후속 재도입용 adapter

### support.persistence
- 공통 base entity
- auditing

### support.redis
- Redis key 유틸
- Redis serializer/config

## 패키지 간 의존 규칙

### 허용
- `api -> application`
- `application -> domain`
- `application -> support`
- `infra -> domain`
- `infra -> support`

### 금지
- `domain -> api`
- `domain -> infra` 직접 의존
- `auth.domain -> payment.infra` 식의 교차 infra 의존
- controller에서 직접 repository 호출

## 도메인 간 호출 규칙

### 1차 권장 방식
- 동기 유스케이스 호출은 application 계층에서만
- 후행 반영은 내부 이벤트 핸들러 사용

### 예시
- `payment.application.PaymentConfirmUseCase`
  - 결제 성공
  - `DomainEventPublisher.publish(new PaymentSucceededEvent(...))`

- `ticket.application.PaymentSucceededHandler`
  - 이벤트 수신
  - 티켓 발급
  - reservation 반영

## 이벤트 패키지 배치 원칙

### 이벤트 클래스 위치
- 이벤트를 발생시키는 도메인 아래 둔다.
- 예: `payment.domain.event.PaymentSucceededEvent`

### 이벤트 핸들러 위치
- 이벤트를 소비하는 쪽 application 패키지에 둔다.
- 예: `ticket.application.PaymentSucceededHandler`

### 이유
- 발행 책임과 소비 책임이 분리된다.
- 기존 Kafka producer/consumer의 책임 분리가 자연스럽게 유지된다.

## 기존 서비스 -> 모놀리식 패키지 매핑

| 기존 서비스 | 모놀리식 대상 패키지 |
| --- | --- |
| Gateway | `support.security`, `support.web` |
| Auth | `auth` |
| Ticketing Performance | `performance` |
| Ticketing Ticket | `ticket` |
| Queue | `queue` |
| RealTime | `seat` |
| Payment | `payment` |
| Reservation | `reservation` |
| Swap | `swap` |

## 파일/클래스 이식 우선순위

### 우선 이식
- controller
- service/use case
- entity
- repository
- redis key manager
- websocket/interceptor

### 나중 이식
- 테스트용 dummy API
- Swagger docs annotation 세부
- 배포용 설정
- 일부 운영 보조 코드

## 추천 네이밍 규칙

### UseCase / Facade
- 외부에서 호출하는 진입점
- 예: `PaymentConfirmUseCase`

### DomainService
- 엔티티 하나로 설명되지 않는 규칙 처리
- 예: `SeatLockService`

### Handler / Projector
- 이벤트 후행 처리
- 예: `PaymentSucceededHandler`, `ReservationProjector`

### Store / Repository
- Redis는 `Store`
- JPA는 `Repository`

## 초기 디렉터리 생성 제안

```text
src/main/java/com/ddib/monolith
src/main/resources
src/test/java/com/ddib/monolith
docs
```

도메인 패키지는 최소한 다음 순서로 생성한다.

1. `support`
2. `auth`
3. `performance`
4. `queue`
5. `seat`
6. `payment`
7. `ticket`
8. `reservation`
9. `swap`

## 결론
- 이 패키지 구조의 핵심은 "서비스를 없애되 경계는 유지"하는 것이다.
- 성능 비교를 위해서는 `queue`, `seat`, `payment`, `ticket`, `reservation` 흐름이 가장 중요하다.
- 따라서 monolith는 단순한 "한 프로젝트"가 아니라, 명확한 도메인 모듈이 공존하는 구조로 시작해야 한다.
