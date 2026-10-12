# 프로젝트 설명 — 주문 시스템으로 알아보는 분산 트랜잭션

> [README](../README.md)로 돌아가기

## 목적

단일 DB·단일 애플리케이션(모놀리식)에서는 `@Transactional` 하나로 보장되던 데이터 정합성이,
서비스가 분리되어 **여러 DB/서비스에 걸친 작업**이 되면 어떻게 깨지는지 확인하고,
이를 해결하기 위한 **분산 트랜잭션 기법**을 직접 구현하며 학습한다.

- 모놀리식 주문 시스템 구현 → 트랜잭션·동시성 문제 확인
- **2PC (Two-Phase Commit)** 적용
- **TCC (Try-Confirm-Cancel)** 적용 (`tcc` 브랜치, 이 브랜치)

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
| Framework | Spring Boot 3.5.3 (Spring Web, Spring Data JPA) |
| Database | MySQL (Docker) |
| Lock | Redis (WSL 로컬 설치, Spring Data Redis) |
| HTTP Client | RestClient + Apache HttpClient 5 (읽기 타임아웃 설정) |
| Retry | Spring Retry (`@Retryable`) |
| Build | Gradle (Kotlin DSL) |

## 프로젝트 구조

```
distributed-transaction        # Gradle 멀티 모듈 루트 (공통 플러그인 버전 관리)
├── common                     # 서비스 공통 라이브러리 (order·product·point가 의존, bootJar 없음)
│   └── src/main/java/com/example/common
│       ├── RedisLockService        # Redis 락 (TTL 10초)
│       ├── LockTransactionExecutor # Redis 락 + 트랜잭션(TransactionTemplate) 실행
│       └── RetryExecutor           # 동시성 실패(ConcurrencyFailureException) 재시도
├── monolithic                 # 모놀리식 주문 시스템
│   └── src/main/java/com/example/monolithic
│       ├── application        # OrderService, ProductService, PointService
│       ├── infrastructure     # JPA Repository
│       └── product/domain     # Order, OrderItem, Product, Point 엔티티
├── order                      # 주문 서비스 (port 8080) — TCC 코디네이터
│   └── src/main/java/com/example/order
│       ├── controller         # OrderController (POST /order, /order/place)
│       ├── application        # OrderCoordinator(Try → Confirm 진행), OrderService(주문 생성·상태 변경)
│       ├── config             # ApiClientConfig (product·point RestClient, 읽기 타임아웃 2초)
│       ├── infrastructure     # JPA Repository, product/ProductApiClient, point/PointApiClient(@Retryable)
│       └── domain             # Order(requestId, 상태), OrderItem
├── product                    # 상품 서비스 (port 8081) — TCC 참여자
│   └── src/main/java/com/example/product
│       ├── controller         # ProductController (POST /products/reserve, /confirm, /cancel)
│       ├── application        # ProductFacadeService(락·트랜잭션·재시도), ProductService(예약·확정·취소)
│       ├── infrastructure     # JPA Repository
│       ├── domain             # Product(reservedQuantity, @Version), ProductReservation(예약 내역)
│       └── init               # TestDataCreator (기동 시 기본 상품 데이터)
└── point                      # 포인트 서비스 (port 8083) — TCC 참여자
    └── src/main/java/com/example/point
        ├── controller         # PointController (POST /points/reserve, /confirm, /cancel)
        ├── application        # PointFacadeService(락·트랜잭션·재시도), PointService(예약·확정·취소)
        ├── infrastructure     # JPA Repository
        ├── domain             # Point(reservedAmount, @Version), PointReservation(예약 내역)
        └── init               # TestDataCreator (기동 시 기본 포인트 데이터)
```

## 실행 방법

### 1. MySQL 실행

```bash
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=1234 --name mysql mysql
```

### 2. Redis 실행

중복 처리를 막는 락에 사용한다. 기본 접속 정보(`localhost:6379`)를 쓰므로 별도 설정은 필요 없다.
WSL에 설치한 Redis(systemd 서비스)를 사용한다.

```bash
sudo apt install redis-server        # 최초 1회
sudo systemctl start redis-server
redis-cli ping                       # PONG 이면 정상
```

> Docker로 Redis를 띄우지 않는다. WSL Redis가 이미 6379 포트를 쓰고 있어서,
> Docker 컨테이너를 함께 띄우면 포트가 연결되지 않고 컨테이너 안에서만 동작한다.

### 3. 설정 파일 생성

이 브랜치에서는 `application.yaml`을 Git에서 관리하지 않는다(`.gitignore`, 로컬 전용).
Git에는 `monolithic`의 `application.yaml.example`(DB 접속 정보 url·username·password를 비워 둠)만 올라가 있으므로, 복사한 뒤 접속 정보를 채운다.

```bash
cp monolithic/src/main/resources/application.yaml.example \
   monolithic/src/main/resources/application.yaml
# url, username, password 입력
```

order·product·point는 예시 파일이 없으므로 각 모듈의 `src/main/resources/application.yaml`을 직접 만든다. 위 예시 형식에 다음을 맞춘다.

- `server.port`: order `8080`, product `8081`, point `8083` (order의 `ApiClientConfig`가 product·point를 이 포트로 호출한다)
- `spring.jpa.hibernate.ddl-auto: create` — `TestDataCreator`가 기동할 때마다 새 테이블에 기본 데이터를 넣는 것을 전제로 한다.

### 4. 테스트 실행

```bash
./gradlew test               # 전체 모듈
./gradlew :monolithic:test   # 모듈 단위
./gradlew :product:test
./gradlew :point:test
```

> 테스트는 실제 MySQL·Redis에 붙는 `@SpringBootTest`이므로 1~3단계가 먼저 되어 있어야 한다.
> 테스트 데이터(포인트, 상품)는 각 테스트 클래스(`DuplicateOrderTest`, `ProductServiceTest`, `PointServiceTest` 등)의 `@BeforeEach`에서 생성한다.

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

같은 방식을 TCC 참여자(product·point)에서도 쓰므로 `common` 모듈의 `LockTransactionExecutor`로 옮겼다.

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

## TCC 구현 상세

주문 흐름·주문 상태·참여자별 Try/Confirm/Cancel 동작은 [README](../README.md#이-브랜치의-tcc-구현)에 정리했다. 여기서는 구현 세부 사항을 다룬다.

### 서비스 구성과 API

| 서비스 | 포트 | 역할 | API |
|---|---|---|---|
| order | 8080 | 코디네이터 | `POST /order` (주문 생성, 응답 `orderId`), `POST /order/place` (주문 처리: Try → Confirm) |
| product | 8081 | 참여자 | `POST /products/reserve` (재고 예약, 응답 `totalPrice`), `POST /products/confirm`, `POST /products/cancel` |
| point | 8083 | 참여자 | `POST /points/reserve` (포인트 예약), `POST /points/confirm`, `POST /points/cancel` |

| 요청 | 필드 |
|---|---|
| `POST /order` | `orderItems: [{productId, quantity}]` → 응답 `{orderId}` |
| `POST /order/place` | `orderId` |
| `POST /products/reserve` | `requestId`, `items: [{productId, reserveQuantity}]` → 응답 `{totalPrice}` |
| `POST /points/reserve` | `requestId`, `userId`, `reserveAmount` |
| `POST /products/confirm`, `/products/cancel`, `/points/confirm`, `/points/cancel` | `requestId` |

- `requestId`는 주문 생성 시 만든 UUID(`Order.requestId`, `CHAR(36)`)다. 한 주문의 예약·확정·취소에 같은 값을 보낸다.
- product는 상품별로 예약 내역을 한 행씩 저장하므로 한 `requestId`에 여러 행이 생긴다. point는 `requestId`당 한 행이다.

### 멱등성과 동시성

| 대상 | 방법 |
|---|---|
| 같은 주문 동시 처리 (order) | `POST /order/place`에서 Redis 락 `order:place:{orderId}`. 이미 처리된 주문은 `Order.reserve()`의 상태 검사(`CREATED`에서만 가능)에서 걸러진다. |
| 같은 요청 동시 처리 (참여자) | `LockTransactionExecutor`의 Redis 락. 키는 `product:reserve:{requestId}`·`product:confirm:{requestId}`·`product:cancel:{requestId}`, point도 같은 형식(`point:reserve:{requestId}` 등). 락 획득 → 트랜잭션 커밋 → 락 해제 순서로 처리한다. |
| 같은 요청 재처리 (참여자) | 예약 내역(`requestId`)과 상태로 판단한다. 아래 표 참고. |
| 다른 요청이 같은 상품·포인트를 동시에 변경 | `Product`·`Point`의 `@Version`(낙관적 락). 먼저 커밋한 요청만 반영되고 나중 요청은 버전 충돌로 실패 → `RetryExecutor`가 재시도한다. |

| 요청 | product | point |
|---|---|---|
| Try 재요청 | 예약 내역이 있으면 새로 예약하지 않고 기존 예약 금액 합계를 반환 | 예약 내역이 있으면 그대로 반환 |
| Confirm 재요청 | 이미 `CONFIRMED`면 그대로 반환 | 이미 `CONFIRMED`면 그대로 반환 |
| Cancel 재요청 | 이미 `CANCELED`면 그대로 반환 | 이미 `CANCELLED`면 예외 (`이미 취소된 예약입니다.`) |
| 예약 내역 없이 Confirm·Cancel | 예외 | 예외 |
| 확정된 예약을 취소 | 예외 (`ProductReservation.cancel`) | 예외 (`PointReservation.cancel`) |

### 재시도

| 위치 | 대상 | 방식 |
|---|---|---|
| 참여자 Facade (`RetryExecutor`) | 락 획득 실패(`CannotAcquireLockException`), 낙관적 락 충돌(`OptimisticLockingFailureException`) | `ConcurrencyFailureException` 계열만 최대 3회, 1초 대기. 모두 실패하면 마지막 예외를 원인으로 담아 작업별 메시지(예: `예약에 실패하였습니다.`, `포인트 확정에 실패하였습니다.`) 예외를 던진다. 수량·금액 부족 등은 재시도하지 않고 원래 예외를 그대로 던진다. |
| order → point (`PointApiClient.reserve`) | 타임아웃 등 일시적 오류 | `@Retryable(maxAttempts = 3, backoff = 500ms)`, `noRetryFor` 400·404. `OrderApplication`의 `@EnableRetry`로 활성화한다. |

- `@Retryable`이 동작하려면 Spring 프록시를 거쳐야 한다. `PointApiClient`는 `ApiClientConfig`의 `@Bean`으로 등록되어 프록시가 적용된다.
- 같은 `requestId`로 다시 보내므로, 앞선 요청이 타임아웃 뒤 point에서 실제로 처리됐더라도 중복 예약되지 않는다.
- 참여자에는 별도 예외 처리기(`@ExceptionHandler`)가 없어 비즈니스 예외도 500으로 응답한다. 그래서 `noRetryFor`(400·404)에 걸리지 않고, 포인트 부족 같은 실패도 재시도된 뒤 실패한다.

### 테스트

| 테스트 클래스 | 테스트 | 검증 내용 |
|---|---|---|
| `ProductServiceTest` | `상품을_예약하면_예약_수량이_증가하고_예약_내역이_저장된다` | 예약 금액(수량 × 가격), 상품의 예약 수량, 예약 내역(수량·금액·`RESERVED`)이 저장된다. |
| | `예약_가능한_수량을_초과하면_재시도하지_않고_원래_예외를_던진다` | 수량 부족 예외가 재시도 없이 원래 메시지 그대로 전달되고, 아무것도 예약되지 않는다. |
| | `예약을_확정하면_재고와_예약_수량이_차감되고_예약_상태가_CONFIRMED가_된다` | 재고 100 → 99, 예약 수량 0, 예약 상태 `CONFIRMED`. |
| | `확정_전에_예약을_취소하면_예약_수량이_원상복구되고_예약_상태가_CANCELED가_된다` | 재고는 그대로, 예약 수량 0, 예약 상태 `CANCELED`. |
| `PointServiceTest` | `포인트를_예약하면_예약_금액이_증가하고_예약_내역이_저장된다` | 포인트는 그대로, 예약 금액 증가, 예약 내역(`RESERVED`) 1건 저장. |
| | `예약을_확정하면_포인트와_예약_금액이_차감되고_예약_상태가_CONFIRMED가_된다` | 포인트 1,000 → 950, 예약 금액 0, 예약 상태 `CONFIRMED`. |
| | `확정_전에_예약을_취소하면_예약_금액이_원상복구되고_예약_상태가_CANCELLED가_된다` | 포인트는 그대로, 예약 금액 0, 예약 상태 `CANCELLED`. |
| `OrderApplicationTests` | `contextLoads` | 컨텍스트 로딩만 확인한다. 코디네이터 흐름 테스트는 없다. |

### 실행

```bash
./gradlew :product:bootRun   # 8081, 기동 시 상품 1(재고 100, 100원)·2(재고 100, 200원) 생성
./gradlew :point:bootRun     # 8083, 기동 시 1번 사용자 10,000 포인트 생성
./gradlew :order:bootRun     # 8080

curl -X POST localhost:8080/order -H 'Content-Type: application/json' \
  -d '{"orderItems":[{"productId":1,"quantity":1},{"productId":2,"quantity":1}]}'   # → {"orderId":1}
curl -X POST localhost:8080/order/place -H 'Content-Type: application/json' -d '{"orderId":1}'
# 동기 처리라 응답 시점에 확정까지 끝난다 → 주문 CONFIRMED, 재고 99개씩, 포인트 9,700
```

- 주문 상태 조회 API는 없으므로 결과는 DB(`orders`, `products`, `point`, 예약 테이블)에서 확인한다.
- 기동 후 첫 `/points/reserve` 요청은 `PointController`의 실습 코드 때문에 2초 대기한다. order의 읽기 타임아웃(2초)에 걸려 `@Retryable`이 다시 보내는 것을 로그(`진입!!`)로 확인할 수 있다.
