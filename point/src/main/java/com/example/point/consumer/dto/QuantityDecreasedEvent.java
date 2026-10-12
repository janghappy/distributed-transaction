package com.example.point.consumer.dto;

public record QuantityDecreasedEvent(Long orderId, Long totalPrice) {
}
