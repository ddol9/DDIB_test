# Benchmark Report Template

## 실험 메타

| 항목 | 값 |
| --- | --- |
| 실험 일시 |  |
| 실험 대상 | `MSA` / `monolith` |
| 브랜치 / 커밋 |  |
| 실행 환경 |  |
| k6 실행 호스트 |  |
| JVM 옵션 |  |
| DB / Redis / Kafka 구성 |  |

## 시나리오 설정

| 변수 | 값 |
| --- | --- |
| `PERFORMANCE_ID` |  |
| `OPTION_ID` |  |
| `MAX_VUS` |  |
| `SEATS_PER_ORDER` |  |
| stage / duration |  |
| `WAIT_POLL_MS` |  |
| `WAIT_MAX_MS` |  |
| `ACTIONS_PER_SESSION` |  |
| `REENTRY_COUNT` |  |

## 결과 요약

### full-flow

| 지표 | MSA | Monolith | 차이 |
| --- | --- | --- | --- |
| `queue_to_token_ms p95` |  |  |  |
| `seat_response_ms p95` |  |  |  |
| `payment_prepare_ms p95` |  |  |  |
| `payment_confirm_ms p95` |  |  |  |
| `reservation_projection_delay_ms p95` |  |  |  |
| `full_flow_success_rate` |  |  |  |

### realtime-stress

| 지표 | MSA | Monolith | 차이 |
| --- | --- | --- | --- |
| `seat_response_ms avg` |  |  |  |
| `seat_response_ms p95` |  |  |  |
| `seat_secure_rate` |  |  |  |
| `ws_connect_failed` |  |  |  |

### queue-refresh

| 지표 | MSA | Monolith | 차이 |
| --- | --- | --- | --- |
| `queue_to_token_ms avg` |  |  |  |
| `queue_to_token_ms p95` |  |  |  |
| `queue_reentry` |  |  |  |
| `short_hold_success_rate` |  |  |  |

## 해석

### 관찰 1
- 

### 관찰 2
- 

### 관찰 3
- 

## 병목 추정

| 구간 | 증상 | 추정 원인 | 확인 방법 |
| --- | --- | --- | --- |
| queue |  |  |  |
| websocket |  |  |  |
| payment |  |  |  |
| reservation projection |  |  |  |

## 보조 지표

### 애플리케이션
- CPU
- Heap
- GC pause
- active threads

### 인프라
- DB connection usage
- Redis ops/sec
- Kafka lag 또는 내부 이벤트 처리시간

## 결론

### 모놀리식이 더 나은 점
- 

### MSA가 더 나은 점
- 

### 다음 액션
1. 
2. 
3. 
