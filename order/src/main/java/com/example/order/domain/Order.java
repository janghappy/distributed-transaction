package com.example.order.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // product·point 서비스에 보내는 요청 ID. 주문마다 한 번 생성해 예약·확정·취소에 같은 값을 써야
    // 각 서비스가 중복 요청을 걸러낼 수 있다. 주문 ID는 DB가 초기화되면 다시 1부터 시작해 다른 주문과 겹칠 수 있어 UUID를 쓴다.
    // 기본 매핑(BINARY(16))은 DB에서 바로 읽을 수 없어, 조회하기 쉽도록 CHAR(36) 문자열로 저장한다.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, unique = true, length = 36)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public void reserve() {
        if(this.status != OrderStatus.CREATED){
            throw new RuntimeException("생성된 단계에서만 예약할 수 있습니다.");
        }
        this.status = OrderStatus.RESERVED;
    }




    public enum OrderStatus {
        CREATED,
        RESERVED,
        CANCELLED,
        CONFIRMED,
        PENDING,
        COMPLETED,
    }
    public Order() {
        this.requestId = UUID.randomUUID();
        this.status = OrderStatus.CREATED;
    }
    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public void confirm() {
        if(this.status != OrderStatus.RESERVED){
            throw new RuntimeException("예약 단계 혹은 PENDING 상태에서만 에서만 확정할 수 있습니다.");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if(this.status != OrderStatus.RESERVED){
            throw new RuntimeException("예약 단계에서만 취소할 수 있습니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void pending() {
        if(this.status != OrderStatus.RESERVED){
            throw new RuntimeException("예약 단계에서만 취소할 수 있습니다.");
        }
        this.status = OrderStatus.PENDING;
    }
}
