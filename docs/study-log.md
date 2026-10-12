# 학습 기록

> [README](../README.md)로 돌아가기
>
> 날짜별로 어느 브랜치에서 무엇을 구현·테스트했는지 기록한다. 코드는 각 브랜치에 있다.
>
> `main` 브랜치(모놀리식·MSA 토대, 학습 허브 문서)는 2026-10-12에 삭제했다. 모놀리식·MSA 토대 코드는 모든 패턴 브랜치에 포함되어 있고, 정리 문서는 `saga-choreography`(기본 브랜치)로 옮겼다.

## 2026-10-12 — `saga-choreography`

SAGA Choreography 흐름을 완성하고 실패 상황을 점검했다.

- point: `quantity-decreased` 구독 → 포인트 차감, 성공 시 `point-used` / 실패 시 차감 취소 후 `point-use-fail` 발행
- order: `point-used` → 주문 `COMPLETED`, `quantity-decreased-fail` → 주문 `FAILED`
- product: `point-use-fail` 구독 → 재고 원복 후 `quantity-decreased-fail` 발행
- 주문 상태 조회 API(`GET /order/{id}/status`) 추가
- Kafka 설정 오타 수정(`support.serializer`, `JsonDeserializer`)과 `application.yaml` git 관리
- 실패 테스트에서 실패 이벤트가 10배로 중복 발행되는 문제 발견
  - 원인: consumer가 예외를 다시 던져 `DefaultErrorHandler`가 재시도함
  - 예외를 다시 던지지 않도록 수정 (강의 코드와 동일)
- README를 TCC·SAGA Orchestration·SAGA Choreography 최종 정리로 재작성, `main` 브랜치 삭제 후 정리 문서를 이 브랜치로 이동

## 2026-10-11 — `saga-choreography`

SAGA 패턴을 **Choreography 방식**으로 구현하기 시작했다. (`saga-orchestration` 코드에서 출발, 메시지 브로커는 Kafka)

- order: 주문 처리 시 주문을 `REQUESTED`로 바꾸고, 트랜잭션 커밋 후(`afterCommit`) `order-placed` 토픽에 `OrderPlacedEvent` 발행
- product: `order-placed` 구독 → 재고 차감
  - 성공 시 `quantity-decreased` 토픽에 `QuantityDecreasedEvent(orderId, totalPrice)` 발행
  - 실패 시 차감 취소 후 `quantity-decreased-fail` 토픽에 `QuantityDecreasedFailEvent` 발행

## 2026-10-11 — `saga-orchestration`

SAGA 패턴을 **Orchestration 방식**으로 구현하고 실패 상황을 테스트했다.

- order에 product·point API 클라이언트 구현 (RestClient + Spring Retry `@Retryable`, 500ms 간격 최대 3회)
- `OrderCoordinator`(오케스트레이터) 구현: 주문 `REQUESTED` → 재고 차감 → 포인트 차감 → `COMPLETED`
- 실패 시 보상 트랜잭션: 재고 차감 취소 → 포인트 차감 취소 → 주문 `FAILED`
- 실행 이력이 없는 취소 요청은 예외 없이 무시하도록 변경 (보상을 어느 단계에서든 호출할 수 있게)
- 실패 상황 테스트: 포인트 차감 실패 시 재고 원복 확인, 보상 자체가 실패하면 `CompensationRegistry(PENDING)`에 재처리 대상으로 기록
- README(SAGA Orchestration 정리)와 `docs/project.md`(구현 상세) 분리

## 2026-10-09 — `tcc`, `saga-orchestration`

- `tcc`: Spring Retry로 일시적인 오류에 대처 — point 예약 호출 재시도, 읽기 타임아웃 2초, 지연 시뮬레이션
- `saga-orchestration`: SAGA 참여자 API 구현
  - product: 재고 차감(`/product/buy`)·차감 취소(`/product/buy/cancel`), 구매·취소 이력으로 멱등성 처리
  - point: 포인트 차감(`/point/use`)·차감 취소(`/point/use/cancel`), 사용·취소 이력으로 멱등성 처리
  - order: 주문 생성 API(`/order`)

## 2026-10-06 — `tcc`

TCC 패턴을 구현했다.

- product·point에 Try(예약)·Confirm(확정)·Cancel(취소) 구현 및 테스트
- 재시도·Redis 락·트랜잭션 패턴을 `common` 모듈로 공통화 (`RetryExecutor`, `LockTransactionExecutor`)
- product·point API(Controller) 구현
- order에 TCC 코디네이터 구현: Try(상품·포인트 예약) → Confirm, 실패 시 Cancel / `PENDING`
- 주문 `requestId`를 UUID로 변경 (주문 ID 재사용으로 인한 요청 충돌 방지)
- 줄바꿈 LF 통일(`.gitattributes`, `.editorconfig`)

## 2026-09-30 — `main`(삭제됨), `tcc`

- `main`: 모놀리식 주문 시스템 구현 (Spring Boot 3.5.3, JPA, MySQL)
- `main`: Redis 락으로 동일 주문 중복 처리 방지, 포인트 부족 시 전체 롤백 검증 테스트
- `main`: MSA 구조 토대 — order·product·point 모듈 분리
- `tcc`: 상품 예약(Try)에 Redis 락·낙관적 락(`@Version`)·재시도 적용
