# 주문 시스템으로 알아보는 분산 트랜잭션

## 목적

단일 DB·단일 애플리케이션(모놀리식)에서는 `@Transactional` 하나로 보장되던 데이터 정합성이,
서비스가 분리되어 **여러 DB/서비스에 걸친 작업**이 되면 어떻게 깨지는지 확인하고,
이를 해결하기 위한 **분산 트랜잭션 기법**을 직접 구현하며 학습한다.

- 모놀리식 주문 시스템 구현 → 트랜잭션·동시성 문제 확인
- **2PC (Two-Phase Commit)** 적용
- **TCC (Try-Confirm-Cancel)** 적용

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
| Build | Gradle (Kotlin DSL) |

## 프로젝트 구조

```
distributed-transaction        # Gradle 멀티 모듈 루트 (공통 플러그인 버전 관리)
├── common                     # 서비스 공통 라이브러리 (서비스 모듈이 의존)
│   └── src/main/java/com/example/common
│       ├── RetryExecutor           # 동시성 실패 재시도 공통 처리
│       ├── LockTransactionExecutor # Redis 락 + 트랜잭션 공통 처리
│       └── RedisLockService        # Redis 락
├── monolithic                 # 모놀리식 주문 시스템
│   └── src/main/java/com/example/monolithic
│       ├── application        # OrderService, ProductService, PointService
│       ├── infrastructure     # JPA Repository
│       └── product/domain     # Order, OrderItem, Product, Point 엔티티
├── order                      # 주문 서비스 (MSA)
├── product                    # 상품 서비스 (MSA, port 8081)
│   └── src/main/java/com/example/product
│       ├── application        # ProductFacadeService(재시도), ProductService(예약·확정·취소)
│       ├── infrastructure     # JPA Repository
│       └── domain             # Product, ProductReservation 엔티티
└── point                      # 포인트 서비스 (MSA)
```

## 실행 방법

### 1. MySQL 실행

```bash
docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=1234 --name mysql mysql
```

### 2. Redis 실행

주문 중복 처리를 막는 락에 사용한다. 기본 접속 정보(`localhost:6379`)를 쓰므로 별도 설정은 필요 없다.
WSL에 설치한 Redis(systemd 서비스)를 사용한다.

```bash
sudo apt install redis-server        # 최초 1회
sudo systemctl start redis-server
redis-cli ping                       # PONG 이면 정상
```

> Docker로 Redis를 띄우지 않는다. WSL Redis가 이미 6379 포트를 쓰고 있어서,
> Docker 컨테이너를 함께 띄우면 포트가 연결되지 않고 컨테이너 안에서만 동작한다.

### 3. 설정 파일 생성

`application.yaml`은 Git에서 관리하지 않는다(로컬 전용).
Git에는 DB 접속 정보(url, username, password)를 비운 `application.yaml.example`만 올라가 있으므로, 복사한 뒤 접속 정보를 채운다.

```bash
cp monolithic/src/main/resources/application.yaml.example \
   monolithic/src/main/resources/application.yaml
# url, username, password 입력
```

### 4. 테스트 실행

```bash
./gradlew test               # 전체 모듈
./gradlew :monolithic:test   # 모듈 단위
./gradlew :product:test
```

> 테스트 데이터(포인트, 상품)는 각 테스트 클래스(`OrderServiceTest`, `DuplicateOrderTest`, `ProductServiceTest` 등)의 `@BeforeEach`에서 생성한다.

## 동일 주문 중복 처리 방지

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

## 상품 예약 (`product` 모듈, TCC의 Try 단계)

`ProductFacadeService.tryReserve` → `ProductService.tryReserve` 순서로 호출되며, 실제 재고를 차감하지 않고
예약 수량(`reservedQuantity`)만 늘린 뒤 예약 내역(`ProductReservation`, 상태 `RESERVED`)을 저장한다.

| 방어 수단 | 막는 상황 | 동작 |
|---|---|---|
| Redis 락 (`LockTransactionExecutor`) | 같은 `requestId`의 예약이 **동시에** 들어온 경우 | `SET product:reserve:{requestId} NX EX 10`으로 락을 잡은 요청만 처리한다. 모놀리식과 같이 락 획득 → `TransactionTemplate`으로 트랜잭션 실행(커밋) → 락 해제 순서로 처리한다. 확정·취소도 같은 방식으로 각각 `product:confirm:{requestId}`, `product:cancel:{requestId}` 키를 쓴다. |
| 예약 내역 확인 | 처리가 끝난 `requestId`가 **다시** 들어온 경우 | 이미 예약 내역이 있으면 새로 예약하지 않고 기존 예약 금액을 반환한다. (멱등성) |
| 낙관적 락 (`Product.@Version`) | **서로 다른 요청**이 같은 상품을 동시에 예약하는 경우 | 먼저 커밋한 요청만 반영되고, 나중 요청은 버전 충돌로 실패한다. |
| 재시도 (`RetryExecutor`) | 락 획득 실패·버전 충돌로 실패한 경우 | `ConcurrencyFailureException` 계열(락 획득 실패 `CannotAcquireLockException`, 낙관적 락 충돌 `OptimisticLockingFailureException`)만 최대 3회 시도하며, 재시도 전에 1초 대기한다. 모두 실패하면 마지막 예외를 원인으로 담아 작업별 실패 메시지(`예약에 실패하였습니다.`, `예약 확정에 실패하였습니다.`, `예약 취소에 실패하였습니다.`) 예외를 던진다. 수량 부족 등 다시 해도 실패할 예외는 재시도하지 않고 그대로 던진다. |

### 검증 (`ProductServiceTest`)

| 테스트 | 검증 내용 |
|---|---|
| `상품을_예약하면_예약_수량이_증가하고_예약_내역이_저장된다` | 예약 금액(수량 × 가격), 상품의 예약 수량, 예약 내역(수량·금액·`RESERVED` 상태)이 저장된다. |
| `예약_가능한_수량을_초과하면_재시도하지_않고_원래_예외를_던진다` | 수량 부족 예외가 재시도 없이 원래 메시지 그대로 전달되고, 아무것도 예약되지 않는다. |
