package com.example.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private Long quantity;
    private Long price;
    private Long reservedQuantity;
    @Version
    private Long version;

    public Product(Long quantity, Long price) {
        this.quantity = quantity;
        this.price = price;
        this.reservedQuantity = 0L;
    }

    public Long reserve(Long requestedQuantity) {
        long reservableQuantity = this.quantity - this.reservedQuantity;

        if(reservableQuantity < requestedQuantity) {
            throw new RuntimeException("예약할 수 있는 수량이 부족합니다.");
        }

        reservedQuantity += requestedQuantity;

        return this.price * requestedQuantity;
    }

    public Long calculatePrice(Long quantity) {
        return price * quantity;
    }

    public void buy(Long quantity) {
        if(this.quantity < quantity) {
            throw new RuntimeException("재고가 부족합니다.");
        }

        this.quantity -= quantity;
    }
}
