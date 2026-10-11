# 주문 시스템으로 알아보는 분산 트랜잭션 — SAGA Orchestration

주문·재고·포인트를 처리하는 주문 시스템을 order·product·point 서비스로 나누고,
이 브랜치(`saga-orchestration`)에서는 **SAGA 패턴의 Orchestration 방식**으로 분산 트랜잭션을 구현·테스트한다.

- 프로젝트 목적·요구사항·기술 스펙·모듈 구조·실행 방법, 구현 상세(API, 멱등성·동시성, 재시도)는 [프로젝트 설명](docs/project.md)에 정리했다.
- TCC 패턴은 `tcc` 브랜치에서 다룬다.

## SAGA란

여러 서비스의 **로컬 트랜잭션을 순차적으로 실행**하고, 중간에 실패하면 이미 완료한 작업을
**보상 트랜잭션(compensating transaction)** 으로 되돌리는 분산 트랜잭션 패턴이다.
각 서비스의 DB 트랜잭션은 독립적으로 바로 커밋되므로, 전체를 하나의 DB 트랜잭션으로 묶지 않는다.

SAGA는 각 단계를 누가 이어 주느냐에 따라 두 가지 방식으로 나뉜다.

| 방식 | 흐름 제어 | 이 저장소 |
|---|---|---|
| **Orchestration** | 오케스트레이터가 각 서비스에 순서대로 요청하고, 실패 시 보상을 지시한다 (중앙 제어) | `saga-orchestration` 브랜치 (이 문서) |
| Choreography | 각 서비스가 이벤트를 발행·구독하며 다음 단계를 이어 간다 (분산 제어) | 다음 브랜치에서 진행 |

## TCC와의 차이 (이 저장소 기준)

| 구분 | TCC (`tcc` 브랜치) | SAGA Orchestration (이 브랜치) |
|---|---|---|
| 참여자 API | Try / Confirm / Cancel 3단계 | 실행(buy·use) / 보상(cancel) 2단계 |
| 자원 처리 | Try에서 **예약**만 하고 Confirm에서 실제 차감 | 실행 단계에서 **바로 차감**, 실패 시 보상으로 원복 |
| 중간 상태 | 예약 수량·금액으로 다른 요청과 분리됨 | 차감된 상태가 다른 요청에 바로 보임 (일시적 불일치) |
| 참여자 데이터 | 예약 테이블 (`RESERVED`/`CONFIRMED`/`CANCELED`) | 거래 이력 테이블 (`PURCHASE`·`USE` / `CANCEL`) |

## 이 브랜치의 Orchestration 구현

order 서비스의 `OrderCoordinator`가 오케스트레이터로서 product(재고)·point(포인트) 서비스를 순서대로 호출하고, 실패하면 보상 트랜잭션을 지시한다.
API·락·재시도 등 구현 상세는 [SAGA Orchestration 구현 상세](docs/project.md#saga-orchestration-구현-상세)를 참고한다.

### 주문 처리 흐름 (`POST /order/place`)

```
OrderController
 └ Redis 락 order:orchestration:{orderId} 획득 (실패 시 예외) ── finally 락 해제
    └ OrderCoordinator.placeOrder
       ├ 주문 CREATED → REQUESTED
       ├ 1) product 재고 차감   (/product/buy)  → 응답 totalPrice
       ├ 2) point 포인트 차감   (/point/use, 금액 = totalPrice)
       ├ 주문 → COMPLETED
       └ 실패 시 rollback() 후 예외 전달
           ├ product 재고 차감 취소 (/product/buy/cancel)
           ├ 취소 금액(totalPrice) > 0 이면 point 포인트 차감 취소 (/point/use/cancel)
           ├ 주문 REQUESTED → FAILED
           └ 보상 자체가 실패하면 CompensationRegistry(PENDING)에 주문 기록 후 예외 전달
```

주문 상태: `CREATED` → `REQUESTED` → `COMPLETED` (실패 시 `FAILED`)

### 보상 트랜잭션

| 참여자 | 실행 | 보상 |
|---|---|---|
| product | 재고(`quantity`) 차감, 구매 이력(`PURCHASE`) 저장 | 구매 이력 기준으로 재고 원복, 취소 이력(`CANCEL`) 저장 |
| point | 포인트(`amount`) 차감, 사용 이력(`USE`) 저장 | 사용 이력 기준으로 포인트 원복, 취소 이력(`CANCEL`) 저장 |

- **보상은 어느 단계에서 실패했든 호출할 수 있어야 한다.**
  상품 차감 전에 실패했을 수도 있으므로, 실행 이력이 없는 취소 요청은 예외를 던지지 않고 그냥 반환한다(product는 취소 금액 0).
  오케스트레이터는 상품 취소 금액이 0보다 클 때만 포인트 취소를 호출한다.
- **보상이 실패하는 경우**도 있다(참여자 장애 등). 이때 주문을 `CompensationRegistry`(`compensation_registries`, 상태 `PENDING`)에 기록해,
  나중에 다시 보상할 수 있게 남겨 둔다.

## 실패 상황 테스트

강제 예외 코드가 주석 처리된 상태로 들어 있어, 주석을 풀고 재기동하면 각 실패 상황을 재현할 수 있다.

| 위치 | 재현되는 상황 | 기대 결과 |
|---|---|---|
| `PointService.use` 끝의 `throw new RuntimeException("강제로 예외 발생")` | 상품 차감 성공 후 포인트 차감 실패 | 재고 원복(`CANCEL` 이력), 주문 `FAILED`, 주문 API는 오류 응답 |
| `ProductService.cancel` 끝의 `if (true) throw ...` | 보상 트랜잭션(재고 차감 취소) 실패 | `compensation_registries`에 주문이 `PENDING`으로 기록, 주문은 `REQUESTED`로 남음 |

> 포인트 차감은 강제 예외로 롤백되므로 포인트 쪽에는 사용 이력이 남지 않는다. 그래서 위 첫 번째 경우에도 포인트 취소는 이력이 없어 그냥 반환된다.

## 다음 단계: Choreography

다음 브랜치에서는 오케스트레이터 없이, 각 서비스가 이벤트(예: `OrderCreated` → `ProductBought` → `PointUsed`, 실패 시 취소 이벤트)를
발행·구독해 흐름을 이어 가는 Choreography 방식을 구현하고 이 방식과 비교한다.
