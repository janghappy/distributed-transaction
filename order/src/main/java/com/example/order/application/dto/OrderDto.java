package com.example.order.application.dto;

import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID requestId,
        List<OrderItem> orderItems
) {
    public record OrderItem(
            Long productId,
            Long quantity
    ) {
    }
}
