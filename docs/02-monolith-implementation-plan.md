# DDIB 모놀리식 전환 구현 계획

## 문서 목적
- 현재 MSA 구조를 단일 애플리케이션으로 옮길 때의 구현 순서와 기준을 정의한다.
- "비즈니스 로직 변경"과 "배포 구조 변경"을 분리해서, 성능 비교 실험이 가능한 최소 경로를 먼저 만든다.

## 목표
- 기존 사용자 플로우를 최대한 유지한 채 서비스 간 네트워크 홉을 제거한다.
- 성능 비교의 기준이 되는 핵심 예매 경로를 먼저 단일 프로세스에서 동작시킨다.
- 이후 Kafka 제거, DB 통합, Swap 확장 같은 후속 작업이 가능한 구조로 시작한다.

## 비목표
- 1차 구현에서 모든 기능을 완전히 동일하게 재현하는 것
- 1차 구현에서 Redis까지 제거하는 것
- 1차 구현에서 DB 스키마를 하나로 완전히 합치는 것
- 1차 구현에서 교환(Swap), 취소/환불, 운영성 기능을 모두 포함하는 것

## 전환 원칙

### 1. API 계약 우선
- 프론트가 호출하는 외부 API 경로는 가능한 그대로 유지한다.
- WebSocket endpoint와 STOMP destination도 가능한 그대로 유지한다.
- k6 스크립트가 큰 수정 없이 붙을 수 있는 수준을 목표로 한다.

### 2. 도메인 경계 유지
- 서비스는 사라지지만 도메인 경계는 유지한다.
- `Auth`, `Queue`, `RealTime`, `Payment`, `Ticketing`, `Reservation`, `Swap`는 각각 독립 패키지/모듈로 유지한다.

### 3. 인프라 변경은 단계적으로
- Redis는 1차 구현에서 유지한다.
- Kafka는 바로 삭제하지 않고 내부 이벤트 계층으로 대체한다.
- DB는 단일 애플리케이션이 접근하되, 초기에는 논리적 분리 상태를 유지한다.

### 4. 비교 가능성 보존
- 예매 성공까지의 흐름은 현재 구조와 최대한 유사하게 유지한다.
- 비교 실험의 핵심은 "네트워크 분산 제거"이지 "비즈니스 재설계"가 아니다.

## 목표 아키텍처 개요

```text
Client
  -> Monolith
    -> auth
    -> performance
    -> queue
    -> seat
    -> payment
    -> ticket
    -> reservation
    -> swap
    -> support
```

### 핵심 차이
- Gateway 제거
- 서비스 간 HTTP 제거
- Kafka consumer/publisher를 내부 이벤트 처리로 대체
- 단일 애플리케이션 내부에서 트랜잭션/도메인 경계 유지

## 1차 MVP 범위

### 포함
- JWT 기반 로그인/인증
- 공연 목록/상세/좌석 메타 조회
- 대기열 등록/상태 조회
- WebSocket 좌석 선점/해제
- 결제 준비/승인
- 결제 성공 후 티켓 발급
- 내 예매 조회용 Reservation read model 생성

### 제외
- 소셜 로그인 실제 연동 세부사항
- Swap 전체 기능
- 결제 취소/환불
- 운영용 Swagger 병합
- 서비스별 독립 배포 설정

## 구현 단계

## Phase 0. 프로젝트 골격 생성
- 새 Spring Boot 모놀리식 프로젝트 생성
- Java 21 기준 설정
- 공통 의존성 정리
- 패키지 루트와 공통 config 구조 생성
- 기본 actuator/logging/prometheus 설정 이동

### 산출물
- 실행 가능한 빈 monolith 앱
- 기본 `application.yaml`
- 공통 예외/응답 규약

## Phase 1. support 계층 구축
- Gateway의 JWT 검증 로직을 monolith 필터 또는 interceptor로 이동
- `X-User-Id`, `X-Username`, `X-User-Role` 해석 방식을 내부 인증 컨텍스트로 변환
- 공통 예외, trace id, argument resolver, serializer 설정 정리

### 구현 포인트
- 더 이상 "헤더 주입 후 다운스트림 전달"이 필요 없다.
- 대신 request-scoped 인증 정보 또는 argument resolver로 통일한다.

### 완료 조건
- 인증이 필요한 API와 아닌 API를 명확히 분리 가능
- 기존 controller 시그니처를 큰 수정 없이 이식 가능

## Phase 2. Auth 이식
- `Auth` 서비스의 도메인/엔티티/리포지토리/서비스를 monolith로 이동
- 사용자 정보 조회와 JWT 발급/재발급 우선 이식
- 실제 소셜 로그인은 stub 또는 기존 방식 유지 중 선택

### 권장
- 1차는 자체 테스트 계정 또는 간단 로그인으로 대체 가능
- 소셜 연동은 성능 비교에 직접 영향이 작으므로 후순위

## Phase 3. Performance 이식
- 공연, 회차, 좌석 메타 API를 먼저 이식
- Ticketing의 `PerformanceService`와 관련 엔티티/리포지토리 이동
- 좌석 메타 Redis 캐시 로직도 그대로 이동

### 완료 조건
- 프론트의 공연 목록/상세/좌석 조회가 monolith에 붙음

## Phase 4. Queue 이식
- Queue의 Redis 기반 waiting queue, active token 발급, SSE 상태 조회 이식
- 스케줄러와 Redis key 구조는 가능한 그대로 유지

### 주의점
- 현재 Queue는 서비스 분리 기준으로 `option:{id}:queueToken:*` 키를 사용한다.
- monolith에서도 초기에는 동일 키를 유지해야 RealTime/Payment 이식이 쉽다.

### 완료 조건
- 대기열 등록
- 토큰 발급
- SSE 상태 조회

## Phase 5. RealTime Seat 이식
- WebSocket/STOMP 설정 이식
- `SeatLockService`, `RedisSeatStore`, `SeatLockExpirationListener` 이동
- Queue token 검증과 좌석 점유/해제 Redis Lua 스크립트 유지

### 주의점
- 이 단계가 지나야 핵심 동시성 비교가 가능하다.
- Redis lock 전략은 바꾸지 않는 편이 비교 실험에 유리하다.

### 완료 조건
- WebSocket 접속
- 좌석 init/lock/release
- token expiration 시 좌석 회수

## Phase 6. Payment 이식
- `PaymentPrepareService`, `PaymentConfirmService`, `PaymentTokenValidator` 이동
- PG 연동 인터페이스 유지
- 결제 성공 시 외부 Kafka 발행 대신 내부 이벤트 publish로 전환

### 핵심 변경
- 기존:
  - Payment -> Kafka -> Ticketing
- 변경 후:
  - Payment -> internal event publisher -> Ticket handler

### 완료 조건
- `prepare`
- `confirm`
- payment success 이벤트 내부 발행

## Phase 7. Ticket + Reservation 이식
- Ticket 발급/히스토리/좌석 sold 처리 로직 이동
- Reservation read model 생성기 이동
- `ticket.issued` 소비자 로직을 내부 이벤트 핸들러로 치환

### 권장 설계
- Ticket 발급은 application service
- Reservation 반영은 projector 또는 event handler

### 완료 조건
- 결제 성공 후 티켓 발급
- 내 예매 조회 가능

## Phase 8. 성능 측정 가능 상태 정리
- 기존 k6 시나리오가 monolith endpoint에 붙는지 검증
- 환경 변수와 base URL만 바꾸면 동일 시나리오 실행 가능하게 정리
- 비교 지표 수집 포인트 정리

### 완료 조건
- MSA vs monolith에 대해 동일 부하 스크립트 실행 가능

## Phase 9. 후속 확장
- Swap 이식
- 결제 취소/환불 이식
- Kafka 완전 제거 또는 adapter 유지
- DB 스키마 통합 검토

## 내부 이벤트 설계 방안

### 목적
- Kafka 의존 consumer/publisher 흐름을 단번에 제거하지 않고, "계약만 유지한 채 transport만 교체"한다.

### 권장 인터페이스

```java
public interface DomainEventPublisher {
    void publish(Object event);
}
```

### 1차 구현
- Spring `ApplicationEventPublisher` 기반 구현
- `@TransactionalEventListener` 사용

### 예시 흐름
- Payment confirm 성공
- `PaymentSucceededEvent` 발행
- Ticket 모듈이 이벤트 수신
- Ticket 발급
- Reservation projector 호출

### 장점
- 현재 Kafka consumer 역할을 내부 핸들러로 거의 1:1 치환 가능
- 후속으로 Kafka adapter를 다시 붙이기도 쉽다.

## DB 전략

### 권장 1차 전략
- 하나의 애플리케이션
- 하나의 MySQL 인스턴스
- 논리적 schema 또는 table prefix 분리 유지

### 이유
- 기존 테이블 구조 이식이 쉽다.
- 서비스별 마이그레이션 충돌을 줄인다.
- 나중에 실제 단일 스키마로 정리할지 결정하기 쉽다.

### 권장 순서
1. 기존 스키마 최대한 유지
2. 애플리케이션 내부 트랜잭션 연결
3. 병목 확인 후 스키마 통합 검토

## Redis 전략

### 1차 구현에서 유지할 항목
- Queue waiting queue
- active token
- user token reverse index
- seat lock
- occupied seats
- sold seats
- seat broadcast pub/sub

### 이유
- 동시성/실시간 성능 비교의 핵심 상태가 Redis에 있다.
- 이걸 같이 바꾸면 MSA 제거 효과와 Redis 전략 변경 효과가 섞인다.

## 리스크와 대응

| 리스크 | 설명 | 대응 |
| --- | --- | --- |
| 과도한 동시 변경 | 서비스 제거와 도메인 재설계를 동시에 하면 실패 확률이 높다 | transport만 먼저 통합 |
| 이벤트 의미 손실 | Kafka consumer를 그냥 service call로 바꾸면 후행 처리 경계가 사라진다 | 내부 이벤트 계층 유지 |
| 인증 흐름 혼란 | Gateway 헤더 주입 방식이 사라짐 | support.security에서 인증 컨텍스트 표준화 |
| 성능 비교 왜곡 | Redis/Kafka/DB를 한 번에 다 바꾸면 원인 분리 불가 | Redis 유지, Kafka는 transport만 교체 |
| Swap 난이도 | Ticketing 내부 API와 비관적 락 구조가 복잡 | MVP 이후 별도 단계로 분리 |

## 우선순위 백로그

### P0
- monolith skeleton
- support/security
- auth
- performance
- queue
- seat realtime
- payment
- ticket issued
- reservation query

### P1
- payment cancel
- refund propagation
- metrics/observability 정리
- k6 monolith 전용 환경 정리

### P2
- swap
- schema consolidation
- Redis abstraction 정리

## 완료 기준
- 프론트의 핵심 예매 경로가 monolith에서 동작
- 기존 k6 시나리오를 monolith에도 적용 가능
- MSA와 monolith의 비교 대상이 동일한 비즈니스 플로우를 유지
- 코드 구조상 후속 단계에서 Swap/취소/환불 확장이 가능
