# Monolith k6

이 디렉터리는 `DDIB_test` monolith 기준 부하 테스트 스크립트를 담는다.

## Scripts
- `full-flow-test.js`: 로그인 -> 대기열 -> 좌석 선점 -> 결제 준비/승인 -> reservation 조회까지 전체 예매 플로우
- `realtime-stress-test.js`: 대기열 진입 후 WebSocket 좌석 선점/해제 경쟁 경로
- `queue-refresh-test.js`: 짧은 좌석 hold를 동반한 대기열 재진입 churn 경로

## Env
기본값은 [`monolith.env.example`](./monolith.env.example)에 정리했다.

예시:

```bash
export $(grep -v '^#' k6/monolith.env.example | xargs)
```

## Run

```bash
k6 run k6/full-flow-test.js
k6 run k6/realtime-stress-test.js
k6 run k6/queue-refresh-test.js
```

결과 JSON은 `k6/results/` 아래에 저장된다.
