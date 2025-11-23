1. Kafka Consumer Group 기반 분산 처리

  application.properties:
  mp.messaging.incoming.login-events.group.id=account-service
  mp.messaging.incoming.login-events.enable.auto.commit=false

  - group.id: 3개 replica가 모두 account-service group 소속
  - partition 분산: Kafka가 자동으로 partition을 3개 replica에 분배
    - replica-1 → partition 0, 3, 6, ...
    - replica-2 → partition 1, 4, 7, ...
    - replica-3 → partition 2, 5, 8, ...
  - auto.commit=false: 처리 완료 후 수동 commit (at-least-once 보장)

  2. Idempotency 기반 중복 방지

  processed_events 테이블:
  CREATE TABLE processed_events (
      event_id     TEXT PRIMARY KEY,    -- 중복 방지
      event_type   TEXT NOT NULL,
      account_id   BIGINT NOT NULL,
      processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
  );

  eventId 생성:
  login:{accountId}:{sessionId}:{timestamp}

  처리 플로우:
  1. isEventProcessed(eventId) 체크 → 이미 처리됐으면 skip
  2. processLoginEvent() 실행
  3. markEventProcessed(eventId) → DB UNIQUE constraint로 원자적 보장
  4. message.ack() → Kafka offset commit

  3. Failover 시나리오

  Case 1: 처리 중 replica 죽음

  replica-1: 이벤트 받음 → 처리 중 → 죽음
  Kafka: offset commit 안 됨 → 다른 replica에 재전송
  replica-2: 같은 이벤트 받음 → isEventProcessed() → 이미 처리됨 → skip

  Case 2: Kafka rebalancing

  replica-1: partition 0 처리 중
  replica-4 추가: Kafka가 partition 재분배
  replica-4: partition 0 할당 → 기존 이벤트 재처리 시도
  → processed_events 테이블에서 중복 방지

  Case 3: 동시에 같은 이벤트 처리 (극히 드묾)

  replica-1: markEventProcessed() 실행
  replica-2: (네트워크 지연 등으로) 같은 이벤트 받음
  → markEventProcessed() 실행 → DB UNIQUE constraint 위반 → false 반환
  → 이미 처리된 것으로 간주

  4. 왜 3번 처리 안 되나?

  Kafka partition 메커니즘:
  - 같은 consumer group 내에서 하나의 partition은 하나의 consumer에만 할당
  - 3개 replica가 있어도 partition이 분산되므로 동시에 같은 메시지를 3번
  받지 않음

  Idempotency 보장:
  - 만약 네트워크 장애/재시도로 중복 전송되어도
  - processed_events.event_id가 PRIMARY KEY라서 두 번째 insert는 실패
  - 실패해도 로직상 문제없음 (이미 처리됨)

  5. Consul/Envoy와의 관계

  - Consul: Service discovery, health check
    - 3개 replica가 각각 Consul에 등록
    - Kafka consumer는 Consul과 무관하게 동작
  - Envoy: L7 proxy, load balancing
    - gRPC 요청은 Envoy가 3개 replica에 분배
    - Kafka consumer는 각 replica가 독립적으로 실행

  Kafka consumer group이 알아서 partition을 분배하므로 Consul/Envoy는 신경
  쓸 필요 없다.

  ---
  테스트 시나리오

  # replica 3개 실행
  ./account-service-1 &
  ./account-service-2 &
  ./account-service-3 &

  # auth-service에서 login 이벤트 발행
  # → Kafka topic: auth.login.events

  # 3개 replica 중 1개만 처리
  # 로그: "Login event processed: accountId=123 sessionId=456"

  # replica-1 강제 종료
  kill <pid-1>

  # Kafka가 partition을 replica-2, replica-3에 재분배
  # 기존 미처리 이벤트는 재전송되지만 processed_events로 중복 방지

  ---
  더 필요한 부분 있으면 말해라. DB transaction 통합하거나, 실제 비즈니스
  로직 추가하거나, proto 공유 구조 잡거나.
