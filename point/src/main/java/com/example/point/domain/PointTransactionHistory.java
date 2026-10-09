package com.example.point.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "point_transaction_histories")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class PointTransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;
    private Long pointId;
    private Long amount;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    public enum TransactionType {
        USE, CANCEL
    }

    public PointTransactionHistory(String requestId, Long pointId, Long amount, TransactionType transactionType) {
        this.requestId = requestId;
        this.pointId = pointId;
        this.amount = amount;
        this.transactionType = transactionType;
    }
}
