package com.example.monolithic.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
        CREATED, COMPLETED
    }

    public Order() {
        this.status = OrderStatus.CREATED;
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }
}
