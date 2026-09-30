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

### 2. 설정 파일 생성

`application.yaml`은 Git에서 관리하지 않는다(로컬 전용).
Git에는 DB 접속 정보(url, username, password)를 비운 `application.yaml.example`만 올라가 있으므로, 복사한 뒤 접속 정보를 채운다.

```bash
cp monolithic/src/main/resources/application.yaml.example \
   monolithic/src/main/resources/application.yaml
# url, username, password 입력
```

### 3. 테스트 실행

```bash
./gradlew :monolithic:test
```

> 테스트 데이터(포인트, 상품)는 `OrderServiceTest`의 `@BeforeEach`에서 생성한다.
