package com.example.order.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;


    public enum OrderStatus {
        CREATED, REQUESTED, COMPLETED, FAILED;

    }
    public Order() {
        this.status = OrderStatus.CREATED;
    }
    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public void request() {
        if(this.status != OrderStatus.CREATED) {
            throw new RuntimeException("잘못된 요청입니다.");
        }

        this.status = OrderStatus.REQUESTED;
    }

    public void fail() {
        if(this.status != OrderStatus.REQUESTED) {
            throw new RuntimeException("잘못된 요청입니다.");
        }

        this.status = OrderStatus.FAILED;
    }
}
