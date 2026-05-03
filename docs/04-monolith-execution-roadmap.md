# DDIB 모놀리식 실행 로드맵

## 문서 목적
- 모놀리식 전환 작업을 실제 구현 순서로 쪼갠다.
- 각 단계의 선행 조건, 산출물, 완료 기준을 명확히 한다.
- 이 문서만 봐도 바로 이슈 생성과 작업 착수가 가능하도록 한다.

## 전제
- 기준 문서
  - `01-current-architecture-analysis.md`
  - `02-monolith-implementation-plan.md`
  - `03-monolith-package-design.md`
- 대상 범위
  - 1차 목표는 핵심 예매 플로우 비교
  - `Swap`, 취소/환불, 운영성 확장은 후순위

## 최종 목표
- 동일한 예매 플로우를 MSA와 monolith에서 모두 실행 가능
- 동일한 k6 시나리오로 성능 비교 가능
- 핵심 비교 경로
  - 공연 조회
  - 대기열 진입
  - 좌석 선점
  - 결제 준비/승인
  - 티켓 발급
  - 내 예매 조회

## 작업 방식

### 브랜치 전략
- `main`: 문서 및 안정 상태
- 구현은 기능 단위 브랜치로 진행
- 권장 브랜치 예시
  - `feature/monolith-bootstrap`
  - `feature/monolith-auth`
  - `feature/monolith-queue-seat`
  - `feature/monolith-payment-ticket`

### 원칙
- 한 번에 한 축만 옮긴다.
- "서비스 제거"와 "도메인 재설계"를 동시에 하지 않는다.
- 각 마일스톤 종료 시 반드시 실행 검증을 남긴다.

## 전체 마일스톤

| 마일스톤 | 목적 | 핵심 결과물 |
| --- | --- | --- |
| M0 | 프로젝트 부트스트랩 | 실행 가능한 monolith skeleton |
| M1 | 공통 보안/웹 계층 정리 | 인증/예외/공통 config |
| M2 | 조회 축 이식 | auth, performance |
| M3 | 경쟁 제어 축 이식 | queue, seat realtime |
| M4 | 결제/발급 축 이식 | payment, ticket, reservation |
| M5 | 성능 비교 준비 | k6 연결, 측정 항목 고정 |
| M6 | 후속 확장 | cancel/refund/swap |

## M0. 프로젝트 부트스트랩

## 목표
- monolith 프로젝트를 생성하고, 이후 모듈을 옮길 수 있는 최소 구조를 만든다.

## 작업 항목
1. Spring Boot 프로젝트 생성
2. Java 21 설정
3. Gradle 의존성 정의
4. 기본 패키지 생성
5. `application.yaml` 초안 생성
6. actuator/prometheus/logging 기본 설정 이동
7. 로컬 실행 프로필 분리

## 산출물
- `src/main/java/com/ddib/monolith/MonolithApplication`
- `build.gradle`
- `src/main/resources/application.yaml`
- 기본 도메인 패키지 디렉터리

## 완료 기준
- `./gradlew bootRun` 실행 가능
- health endpoint 확인 가능
- 애플리케이션 기동 시 DB/Redis 연결 기본 설정 확인 가능

## 선행 조건
- 없음

## 의존 리스크
- 없음

## M1. 공통 보안/웹 계층 정리

## 목표
- Gateway 제거 후 필요한 인증, 예외, request context 처리를 monolith 내부 공통 계층으로 만든다.

## 작업 항목
1. JWT provider 이동
2. 인증 필터 또는 interceptor 구현
3. `@UserId` argument resolver 이동
4. 공통 exception handler 이동
5. trace id filter/interceptor 이동
6. CORS/config 정리
7. 공통 API 에러 응답 형식 확정

## 산출물
- `support.security`
- `support.exception`
- `support.web`
- 인증/인가가 적용된 샘플 API

## 완료 기준
- 보호 API와 공개 API가 구분 동작
- `Authorization: Bearer ...`로 사용자 식별 가능
- 기존 controller 시그니처를 무리 없이 이식 가능한 상태

## 선행 조건
- M0 완료

## 검증
- 인증 없는 엔드포인트 접근 성공
- 인증 필요한 엔드포인트에서 정상/비정상 토큰 분기 확인

## M2. 조회 축 이식

## 목표
- 사용자 로그인과 공연 조회를 monolith에서 동작시킨다.

## 범위
- `auth`
- `performance`

## 작업 항목
1. Auth 엔티티/리포지토리 이동
2. Auth 서비스 이동
3. Auth/User controller 이동
4. Performance 엔티티/리포지토리 이동
5. Performance service/controller 이동
6. 좌석 메타 Redis 캐시 이동

## 산출물
- 로그인/토큰 발급 API
- 사용자 조회 API
- 공연 목록/상세/좌석 조회 API

## 완료 기준
- 프론트에서 공연 목록/상세가 monolith API로 조회 가능
- 좌석 메타 조회가 정상 응답
- 인증 계정으로 사용자 정보 조회 가능

## 선행 조건
- M1 완료

## 검증
- `GET /api/ticketing/performances`
- `GET /api/ticketing/performances/{performanceId}`
- `GET /api/ticketing/performances/{performanceId}/options/{optionId}/seats`
- `GET /api/users/me`

## M3. 경쟁 제어 축 이식

## 목표
- 현재 구조의 핵심 병목인 queue + realtime seat lock을 monolith에서 재현한다.

## 범위
- `queue`
- `seat`

## 작업 항목
1. Queue Redis key 구조 이동
2. Queue repository/service/controller 이동
3. Queue scheduler 이동
4. Seat WebSocket config 이동
5. Seat Redis store/Lua script 이동
6. Token validation 연결
7. Seat expiration listener 이동
8. Broadcast/pubsub 연결

## 산출물
- 대기열 등록 API
- SSE 상태 API
- WebSocket 연결
- seat init/lock/release

## 완료 기준
- 대기열 등록 후 queue token 발급 가능
- 발급된 token으로 WebSocket 연결 가능
- 좌석 선점/해제/만료 복구가 동작

## 선행 조건
- M2 완료

## 검증
- 단일 사용자 seat lock/release
- 동시 사용자 seat contention
- token 만료 후 좌석 자동 회수

## 주의
- 이 단계에서는 Redis 구조를 바꾸지 않는다.
- 그래야 MSA 대비 monolith의 차이를 분리해 측정할 수 있다.

## M4. 결제/발급 축 이식

## 목표
- 핵심 예매 완료 경로를 monolith에서 끝까지 연결한다.

## 범위
- `payment`
- `ticket`
- `reservation`

## 작업 항목
1. Payment 엔티티/리포지토리 이동
2. Payment prepare/confirm API 이동
3. Payment token validator 이동
4. PG client adapter 이동
5. 내부 이벤트 계층 추가
6. Payment success handler 구현
7. Ticket issue service 이동
8. Reservation projector/handler 구현
9. 내 예매 조회 API 이동

## 산출물
- `POST /api/payments/prepare`
- `POST /api/payments/confirm`
- payment success -> ticket issued -> reservation projected
- `GET /api/reservations/my`

## 완료 기준
- 결제 성공 후 티켓 발급
- sold seat가 Redis에 반영
- 내 예매 조회에서 발급 결과 확인 가능

## 선행 조건
- M3 완료

## 검증
- prepare 성공
- confirm 성공
- 같은 seat에 대한 중복 발급 방지
- reservation 조회 반영 확인

## 주의
- Kafka consumer를 바로 삭제하지 말고 역할을 내부 이벤트 handler로 옮긴다.
- 이벤트 이름과 의미는 최대한 유지한다.

## M5. 성능 비교 준비

## 목표
- MSA와 monolith를 동일 기준으로 비교 가능한 상태로 만든다.

## 작업 항목
1. k6 base URL을 monolith용으로 분리
2. 필수 데이터 seed 방식 정리
3. 테스트 환경 변수 문서화
4. 측정 메트릭 확정
5. 비교 결과 기록 템플릿 작성

## 산출물
- monolith 실행 가이드
- k6 실행 가이드
- 비교 결과 기록 문서 템플릿

## 완료 기준
- 같은 시나리오를 MSA와 monolith에 각각 실행 가능
- 최소 다음 지표를 같은 기준으로 수집 가능
  - queue to token latency
  - seat lock p95
  - payment confirm latency
  - reservation projection delay

## 선행 조건
- M4 완료

## 검증
- `full-flow-test`
- `realtime-stress-test`
- `queue-refresh-test`

## M6. 후속 확장

## 목표
- 핵심 비교 이후 필요한 도메인을 확장한다.

## 범위
- payment cancel
- refund propagation
- swap
- schema consolidation

## 작업 항목
1. Payment cancel 이식
2. Ticket cancel/refund flow 이식
3. Reservation refunded projection 이식
4. Swap domain 이식
5. Ticket swap handler 이식
6. 필요 시 DB 스키마 정리

## 완료 기준
- 핵심 예매 플로우 외의 보조 플로우까지 monolith에서 동작

## 권장 작업 순서

1. M0
2. M1
3. M2
4. M3
5. M4
6. M5
7. M6

이 순서를 깨면 보통 다음 문제가 생긴다.
- M3 이전에 payment를 붙이면 queue token 검증이 어정쩡해짐
- M4 이전에 k6를 붙이면 성공 플로우가 끊김
- M6를 먼저 하면 핵심 비교 일정이 밀림

## 작업 분해 예시

### Epic 1. Bootstrap
- project init
- support config
- security baseline

### Epic 2. Read Path
- auth
- user
- performance

### Epic 3. Contention Path
- queue
- realtime websocket
- redis seat state

### Epic 4. Booking Completion
- payment
- ticket issue
- reservation projection

### Epic 5. Benchmark
- k6 monolith adaptation
- metrics capture
- comparison report

## 체크리스트

### 코드 구조 체크
- [ ] 패키지 구조가 `03` 문서와 일치
- [ ] controller가 repository를 직접 호출하지 않음
- [ ] 도메인 간 후행 처리가 내부 이벤트로 분리됨

### 기능 체크
- [ ] 공연 조회
- [ ] 대기열 진입
- [ ] queue token 발급
- [ ] WebSocket 연결
- [ ] seat lock/release
- [ ] payment prepare
- [ ] payment confirm
- [ ] ticket issue
- [ ] reservation query

### 비교 준비 체크
- [ ] k6 스크립트 monolith 연결 확인
- [ ] 측정 지표 정의
- [ ] 테스트 데이터 준비 방법 문서화

## 의사결정 가이드

### Redis를 유지할지
- 1차는 유지
- 이유: 동시성 전략 비교가 가능해야 함

### Kafka를 유지할지
- 외부 broker는 제거 가능
- 하지만 이벤트 경계 자체는 유지

### DB를 합칠지
- 1차는 논리 분리 유지
- 나중에 실제 병목이 DB join/transaction overhead인지 확인 후 판단

### Swap을 언제 붙일지
- 핵심 예매 플로우 비교가 끝난 뒤

## 예상 산출물 순서

1. monolith skeleton repository
2. 공통 support 계층
3. 조회 가능 상태
4. queue + realtime 동작 상태
5. payment + ticket + reservation 완료 상태
6. k6 비교 결과 문서

## Done 정의

이 로드맵 기준으로 "1차 목표 완료"는 아래를 만족하는 상태다.

1. monolith 하나만 띄워서 핵심 예매 플로우가 끝까지 동작한다.
2. 기존 MSA와 동일한 예매 흐름으로 k6 부하를 줄 수 있다.
3. 최소 한 번 이상의 MSA vs monolith 비교 결과를 문서화했다.
