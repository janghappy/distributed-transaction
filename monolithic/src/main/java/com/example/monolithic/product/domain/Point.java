package com.example.monolithic.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Entity
@Table(name = "point")
public class Point {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long amount;

    public void use(long amount){
        if(this.amount < amount){
            throw new RuntimeException("잔액이 부족합니다.");
        }

        this.amount -= amount;
    }

    public Point(Long userId, Long amount) {
        this.userId = userId;
        this.amount = amount;
    }
}
