package com.example.point.domain;

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
    @Column(nullable = false)
    private Long amount;
    @Column(nullable = false)
    private Long reservedAmount;
    @Version
    private Long version;

    public Point(Long userId, Long amount) {
        this.userId = userId;
        this.amount = amount;
        this.reservedAmount = 0L;
    }

    public void reserve(Long reserveAmount) {
        long reservableAmount = this.amount - this.reservedAmount;

        if(reservableAmount < reserveAmount){
            throw new RuntimeException("금액이 부족합니다.");
        }

        reservedAmount += reserveAmount;
    }

    public void use(long amount){
        if(this.amount < amount){
            throw new RuntimeException("잔액이 부족합니다.");
        }

        this.amount -= amount;
    }

    public void confirm(Long reserveAmount) {
        if(this.amount < reserveAmount){
            throw new RuntimeException("포인트가 부족합니다");
        }

        if(this.reservedAmount < reserveAmount){
            throw new RuntimeException("예약된 금액이 부족합니다.");
        }

        this.amount -= reserveAmount;
        this.reservedAmount -= reserveAmount;
    }

    public void cancel(Long reserveAmount) {
        if(this.reservedAmount < reserveAmount){
            throw new RuntimeException("예약된 금액이 부족합니다.");
        }
        this.reservedAmount -= reserveAmount;
    }
}
