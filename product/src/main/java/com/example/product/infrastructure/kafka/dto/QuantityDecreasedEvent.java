package com.example.product.infrastructure.kafka.dto;

public record QuantityDecreasedEvent(Long orderId, Long totalPrice) {
}
