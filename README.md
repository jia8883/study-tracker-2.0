Kafka 기반 Event Driven Architecture로
Scheduler 병목 문제를 해결한 Slack 기반 학습 트래킹 서비스

# Study Tracker 2.0
※ 기존 1.0 프로젝트의 아키텍처 개선 버전으로, 구조 변경을 명확히 보여주기 위해 별도 repository로 분리함

Study Tracker 2.0은 Slack 기반 학습 기록 서비스로,
Scheduler 중심의 동기 처리 구조에서 발생하던 병목 문제를 해결하기 위해
Kafka 기반 Event Driven Architecture로 리디자인함

- Slack 메시지로 학습 기록
- OpenAI를 통해 요약/피드백 생성
- Slack DM으로 자동 전달
- Kafka 기반 Worker 아키텍처로 비동기 처리

### 1.0에서 발견된 문제

- Scheduler 중심 순차 처리로 인한 처리 지연 및 병목 발생
- 외부 API(OpenAI, Slack) 호출이 포함된 동기 흐름으로 인해
  latency가 전체 처리 시간으로 확장
- WebClient 재시도 폭주로 인한 커넥션 리소스 고갈

### 기존 구조

```js
Scheduler
↓
DB 조회
↓
OpenAI 호출
↓
Slack 전송
```

### 2.0 아키텍처

![architecture](./img/architecture-2.0.png)

2.0에서는 Kafka 기반 이벤트 흐름으로 구조를 변경
Scheduler는 작업을 직접 수행하지 않고,
요약 요청 이벤트를 Kafka로 발행하는 역할만 수행

### 핵심 개선점

| 항목 | 1.0 | 2.0 |
|------|-----|-----|
| 처리 방식 | Scheduler 중심 | Event Driven |
| OpenAI 호출 | Scheduler | Worker |
| Slack 전송 | Scheduler | Worker |
| 메시징 | 없음 | Kafka |
| 재시도 | Redis queue | Redis queue |

### 설계 포인트

- Kafka 기반 비동기 Worker 구조
- OpenAI / Slack API 호출 분리
- 외부 API latency 영향 최소화
- Redis retry queue 유지
- 작업 단위를 이벤트로 분리하여 병렬 처리 가능하도록 설계
- 각 단계(Worker)를 독립적인 Consumer로 분리하여 장애 격리
- 외부 API 호출이 전체 처리 흐름에 영향을 주지 않도록 구조 개선

### 왜 Kafka를 선택했는가

1. Scheduler 기반 구조의 병목을 해소하기 위한 비동기 처리 구조가 필요
2. 외부 API(OpenAI, Slack) 호출을 분리하여 latency 영향을 격리 가능
3. 작업을 이벤트 단위로 분리하여 병렬 처리 및 확장성을 확보
4. Producer / Consumer 구조를 통해 역할을 분리 가능

### 기술 스택
- Language: Java 21
- Framework: Spring Boot 3
- Messaging: Kafka
- Database: PostgreSQL
- Retry: Redis
- External API: Slack API, OpenAI API

### Retry 전략

- 요약 생성 실패 시 Redis 기반 retry queue를 사용
- 최대 재시도 횟수 제한
- 일정 시간 간격으로 재시도 수행
- 최소 1회 이상 처리(at-least-once)

### 향후 개선 계획

- Kafka Retry / Dead Letter Queue 적용

- k6 기반 부하 테스트

- 1.0 vs 2.0 성능 비교

### 관련 문서
- [Study Tracker 1.0 Repository](https://github.com/jia8883/study-tracker)
- [Study Tracker 2.0 아키텍처 리디자인](./reports/study_tracker_2.0_redesign.pdf)


※ 부하 테스트에서는 OpenAI API를 mock 처리했으며,
외부 API latency는 실제 측정값이 아닌 구조적 영향 관점에서 분석함


