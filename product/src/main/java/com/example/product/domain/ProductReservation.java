package com.example.product.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "product_reservations")
public class ProductReservation {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private String requestId;
    private Long productId;
    private Long reservedQuantity;
    private Long reservedPrice;
    @Enumerated(EnumType.STRING)
    private ProductReservationStatus status;

    public enum ProductReservationStatus {
        RESERVED, CONFIRMED, CANCELED
    }

    public ProductReservation(String requestId, Long productId, Long reservedQuantity, Long reservedPrice) {
        this.requestId = requestId;
        this.productId = productId;
        this.reservedQuantity = reservedQuantity;
        this.reservedPrice = reservedPrice;
        this.status = ProductReservationStatus.RESERVED;
    }
}
