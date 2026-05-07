# DDIB Test

이 저장소는 `DDIB` 티켓팅 MSA를 모놀리식 구조로 마이그레이션하고, 동일한 예매 플로우에서 성능을 비교하기 위한 작업 공간이다.

## Docs
- `docs/01-current-architecture-analysis.md`: 현재 MSA 구조 분석
- `docs/02-monolith-implementation-plan.md`: 모놀리식 전환 구현 계획
- `docs/03-monolith-package-design.md`: 모놀리식 패키지/모듈 설계
- `docs/04-monolith-execution-roadmap.md`: 실제 구현 로드맵과 체크리스트
- `docs/05-monolith-benchmark-guide.md`: monolith 실행 및 k6 비교 가이드
- `docs/06-benchmark-report-template.md`: 비교 결과 기록 템플릿

## k6
- `k6/full-flow-test.js`: 전체 예매 플로우 benchmark
- `k6/realtime-stress-test.js`: 좌석 경쟁 benchmark
- `k6/queue-refresh-test.js`: 대기열 재진입 churn benchmark
- `k6/README.md`: 실행 방법
