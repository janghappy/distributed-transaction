package com.example.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "product_transaction_histories")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductTransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;
    private Long productId;
    private Long quantity;
    private Long price;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    public enum TransactionType {
        PURCHASE, CANCEL
    }

    public ProductTransactionHistory(String requestId, Long productId, Long quantity, Long price, TransactionType transactionType) {
        this.requestId = requestId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.transactionType = transactionType;
    }
}
