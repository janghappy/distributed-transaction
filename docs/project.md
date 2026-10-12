# 프로젝트 설명 — 주문 시스템으로 알아보는 분산 트랜잭션

> [README](../README.md)로 돌아가기

## 목적

단일 DB·단일 애플리케이션(모놀리식)에서는 `@Transactional` 하나로 보장되던 데이터 정합성이,
서비스가 분리되어 **여러 DB/서비스에 걸친 작업**이 되면 어떻게 깨지는지 확인하고,
이를 해결하기 위한 **분산 트랜잭션 기법**을 직접 구현하며 학습한다.

- 모놀리식 주문 시스템 구현 → 트랜잭션·동시성 문제 확인
- **2PC (Two-Phase Commit)** 적용
- **TCC (Try-Confirm-Cancel)** 적용 (`tcc` 브랜치)
- **SAGA** 적용 — Orchestration 방식(`saga-orchestration` 브랜치), Choreography 방식(`saga-choreography` 브랜치, 이 브랜치)

## 기능

- 주문 데이터 저장
- 재고 관리 (상품 구매 시 재고 차감)
- 포인트 사용 (주문 금액만큼 포인트 차감)

## 주요 요구사항

- 주문, 재고, 포인트 데이터의 **정합성이 맞아야 한다.**
- 동일한 주문은 **1번만** 이루어져야 한다. (멱등성)

## 기술 스펙

| 구분 | 사용 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.3 (Spring Data JPA) |
| Database | MySQL (Docker) |
| Lock | Redis (WSL 로컬 설치, Spring Data Redis) |
| Retry | Spring Retry (`@Retryable`) — orchestration용 API 클라이언트 |
| Messaging | Apache Kafka (Docker, Spring for Apache Kafka) |
| Build | Gradle (Kotlin DSL) |

## 프로젝트 구조

```
distributed-transaction        # Gradle 멀티 모듈 루트 (공통 플러그인 버전 관리)
├── monolithic                 # 모놀리식 주문 시스템
│   └── src/main/java/com/example/monolithic
│       ├── application        # OrderService, ProductService, PointService
│       ├── infrastructure     # JPA Repository
│       └── product/domain     # Order, OrderItem, Product, Point 엔티티
├── order                      # 주문 서비스 (port 8080) — 주문 이벤트 발행, 결과 이벤트 구독
│   └── src/main/java/com/example/order
│       ├── controller         # OrderController (POST /order, /order/place, GET /order/{id}/status)
│       ├── application        # OrderService(주문 생성·처리·상태 변경·조회)
│       ├── consumer           # PointUsedConsumer(→ COMPLETED), QuantityDecreasedFailConsumer(→ FAILED)
│       ├── infrastructure     # JPA Repository, kafka/OrderPlaceProducer(order-placed 발행)
│       └── domain             # Order, OrderItem
├── product                    # 상품 서비스 (port 8081) — SAGA 참여자
│   └── src/main/java/com/example/product
│       ├── consumer           # OrderPlacedConsumer(재고 차감), PointUseFailConsumer(재고 원복)
│       ├── application        # ProductService(재고 차감·차감 취소)
│       ├── infrastructure     # JPA Repository, kafka/QuantityDecreasedProducer·QuantityDecreasedFailProducer
│       ├── domain             # Product, ProductTransactionHistory(구매·취소 이력)
│       └── init               # TestDataCreator (기동 시 기본 상품 데이터)
└── point                      # 포인트 서비스 (port 8083) — SAGA 참여자
    └── src/main/java/com/example/point
        ├── consumer           # QuantityDecreasedConsumer(포인트 차감)
        ├── application        # PointService(포인트 차감·차감 취소)
        ├── infrastructure     # JPA Repository, kafka/PointUsedProducer·PointUseFailProducer
        ├── domain             # Point, PointTransactionHistory(사용·취소 이력)
        └── init               # TestDataCreator (기동 시 기본 포인트 데이터)
```

> `saga-orchestration` 브랜치에서 시작했기 때문에 orchestration용 코드도 남아 있다.
> 해당 코드는 order의 `OrderCoordinator`·`ProductApiClient`·`PointApiClient`·`CompensationRegistry`, product·point의 HTTP API(`/product/buy`, `/point/use` 등)이다.
> Choreography 흐름(`/order/place`)에서는 사용하지 않는다.

## 실행 방법

### 1. MySQL 실행

```bash
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=1234 --name mysql mysql
```

### 2. Redis·Kafka 실행

주문 중복 처리를 막는 락에 사용한다. 기본 접속 정보(`localhost:6379`)를 쓰므로 별도 설정은 필요 없다.
WSL에 설치한 Redis(systemd 서비스)를 사용한다.

```bash
sudo apt install redis-server        # 최초 1회
sudo systemctl start redis-server
redis-cli ping                       # PONG 이면 정상
```

> Docker로 Redis를 띄우지 않는다. WSL Redis가 이미 6379 포트를 쓰고 있어서,
> Docker 컨테이너를 함께 띄우면 포트가 연결되지 않고 컨테이너 안에서만 동작한다.

Kafka는 Docker 컨테이너(`localhost:9092`)로 실행한다. 토픽은 첫 발행 시 자동 생성된다.

### 3. 설정 파일

각 모듈의 `application.yaml`을 Git으로 관리한다. 접속 대상이 모두 로컬(MySQL `127.0.0.1:3306`, Redis `localhost:6379`, Kafka `localhost:9092`)이고
학습용 계정이라 별도 설정 파일을 만들 필요 없이 그대로 실행한다.

### 4. 테스트 실행

```bash
./gradlew :monolithic:test
```

> 테스트 데이터(포인트, 상품)는 각 테스트 클래스(`OrderServiceTest`, `DuplicateOrderTest`)의 `@BeforeEach`에서 생성한다.

## 동일 주문 중복 처리 방지 (모놀리식)

요구사항 "동일한 주문은 1번만 이루어져야 한다"를 위해, 주문 생성(`createOrder`)과 주문 처리(`placeOrder`)를 분리하고
`placeOrder`에서 두 겹으로 중복 처리를 막는다.

| 방어 수단 | 막는 상황 | 동작 |
|---|---|---|
| Redis 락 (`RedisLockService`) | 같은 주문이 **동시에** 들어온 경우 | `SET order:monolithic:{orderId} NX EX 10`으로 락을 잡은 요청만 처리하고, 나머지는 예외로 거절한다. 처리가 끝나면 `finally`에서 락을 해제한다. |
| 주문 상태 확인 (`COMPLETED`) | 처리가 끝난 주문이 **다시** 들어온 경우 | 이미 완료된 주문이면 재고·포인트를 차감하지 않고 반환한다. |

### 구현 시 주의점

- **락은 트랜잭션 바깥에서 잡고 푼다.** `placeOrder`에 `@Transactional`을 붙이면 `finally`의 락 해제가 DB 커밋보다 먼저 실행된다.
  그 사이 들어온 요청이 락을 잡고 커밋 전 상태(`COMPLETED` 아님)를 읽어 중복 처리할 수 있으므로,
  락 획득 → `TransactionTemplate`으로 트랜잭션 실행(커밋) → 락 해제 순서로 처리한다.
- **락에 만료 시간(TTL 10초)을 둔다.** 락을 잡은 서버가 해제 전에 죽어도 락이 영구히 남지 않게 한다.
  TTL은 주문 처리 시간보다 충분히 길어야 처리 도중 만료되지 않는다.

### 검증 (`DuplicateOrderTest`)

| 테스트 | 검증 내용 |
|---|---|
| `동일한_주문을_2번_요청해도_1번만_처리된다` | 순차로 2번 처리 → 두 번째 요청은 주문 상태 확인에서 걸러진다. |
| `동일한_주문을_동시에_요청해도_1번만_처리된다` | 5개 스레드가 동시에 처리 → 1건만 성공하고 4건은 락 획득 실패로 거절된다. |

두 경우 모두 재고·포인트가 1번만 차감되는지 확인한다.

### 검증 (`OrderRollbackTest`)

| 테스트 | 검증 내용 |
|---|---|
| `포인트가_부족하면_재고_차감도_롤백된다` | 재고 차감 후 포인트 사용에서 실패 → 재고·포인트·주문 상태가 모두 원래대로 돌아온다. |

## SAGA Choreography 구현 상세

주문 흐름과 토픽별 발행·구독은 [README](../README.md#이-브랜치의-choreography-구현)에 정리했다. 여기서는 구현 세부 사항을 다룬다.

### 서비스 구성과 API

| 서비스 | 포트 | 역할 | API / 토픽 |
|---|---|---|---|
| order | 8080 | 주문 시작·결과 반영 | `POST /order` (주문 생성, 응답 `orderId`), `POST /order/place` (주문 처리 → `order-placed` 발행), `GET /order/{id}/status` (주문 상태 조회) / 구독: `point-used`, `quantity-decreased-fail` |
| product | 8081 | 참여자 | 구독: `order-placed`, `point-use-fail` / 발행: `quantity-decreased`, `quantity-decreased-fail` |
| point | 8083 | 참여자 | 구독: `quantity-decreased` / 발행: `point-used`, `point-use-fail` |

이벤트 DTO는 서비스마다 따로 두고(패키지가 다름), 필드 이름으로만 맞춘다.

| 이벤트 | 필드 |
|---|---|
| `OrderPlacedEvent` | `orderId`, `productInfos: [{productId, quantity}]` |
| `QuantityDecreasedEvent` | `orderId`, `totalPrice` |
| `QuantityDecreasedFailEvent`, `PointUsedEvent`, `PointUseFailEvent` | `orderId` |

### Kafka 설정 (`application.yaml`)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.use.type.headers: false   # 보낸 쪽 클래스명(타입 헤더)을 쓰지 않음
        spring.json.trusted.packages: "*"
```

- 받을 타입은 각 `@KafkaListener`의 `properties`에 `spring.json.value.default.type = <이벤트 클래스>`로 지정한다.
- 메시지 키는 `orderId`라서 같은 주문의 이벤트는 같은 파티션에 순서대로 쌓인다.

### 멱등성과 동시성

| 대상 | 방법 |
|---|---|
| 같은 주문 동시 처리 | order의 Redis 락 `order:choreography:{orderId}`. 이미 처리된 주문은 `Order.request()`의 상태 검사(`CREATED`에서만 가능)에서 걸러진다. |
| 이벤트 중복 수신 (참여자) | `orderId`를 `requestId`로 사용한다. 같은 `requestId`의 차감(`PURCHASE`·`USE`)·취소(`CANCEL`) 이력이 있으면 다시 처리하지 않는다. |
| 이벤트 중복 수신 (order) | 별도 처리 없음 (강의 범위 밖). `Order.fail()`은 `REQUESTED`에서만 가능해서 중복 실패 이벤트는 예외로 거절된다. |

### 실패 처리와 재시도

- consumer는 실패하면 자기 작업을 취소하고 실패 이벤트를 발행한 뒤 **정상 종료**한다(예외를 다시 던지지 않음).
  - 예외를 다시 던지면 Kafka `DefaultErrorHandler`가 같은 메시지를 최대 10번(처음 1회 + 재시도 9회) 다시 처리한다.
  - 그러면 실패 이벤트가 그만큼 중복 발행된다(실제 확인: 실패 1건에 `point-use-fail`·`quantity-decreased-fail` 각 10건).
- 실행 이력이 없는 취소 요청은 예외 없이 반환하므로, 어느 단계에서 실패했든 취소를 호출할 수 있다.
- order는 `REQUESTED` 상태 변경을 커밋한 뒤(`afterCommit`) `order-placed`를 발행한다. 그래서 DB에는 없는 주문 요청 이벤트가 나가지 않는다.

### 실행

```bash
./gradlew :product:bootRun   # 8081, 기동 시 상품 1(재고 100, 100원)·2(재고 100, 200원) 생성
./gradlew :point:bootRun     # 8083, 기동 시 1번 사용자 10,000 포인트 생성
./gradlew :order:bootRun     # 8080

curl -X POST localhost:8080/order -H 'Content-Type: application/json' \
  -d '{"items":[{"productId":1,"quantity":1},{"productId":2,"quantity":1}]}'   # → {"orderId":1}
curl -X POST localhost:8080/order/place -H 'Content-Type: application/json' -d '{"orderId":1}'
curl localhost:8080/order/1/status    # 비동기 처리라 잠시 후 COMPLETED / FAILED
```
