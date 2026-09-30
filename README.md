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
└── monolithic                 # 모놀리식 주문 시스템
    └── src/main/java/com/example/monolithic
        ├── application        # OrderService, ProductService, PointService
        ├── infrastructure     # JPA Repository
        └── product/domain     # Order, OrderItem, Product, Point 엔티티
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
./gradlew :monolithic:test
```

> 테스트 데이터(포인트, 상품)는 각 테스트 클래스(`OrderServiceTest`, `DuplicateOrderTest`)의 `@BeforeEach`에서 생성한다.

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
