package com.example.event;

public record QuantityDecreasedEvent(Long orderId, Long totalPrice) {
}
