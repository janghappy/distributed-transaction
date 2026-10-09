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

    @Version
    private Integer version;

    public Product(Long price, Long quantity) {
        this.price = price;
        this.quantity = quantity;
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
