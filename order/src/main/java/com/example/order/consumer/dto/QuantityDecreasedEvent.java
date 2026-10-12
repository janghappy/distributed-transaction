package com.example.order.consumer.dto;

public record QuantityDecreasedEvent(Long orderId, Long totalPrice) {
}
