package com.example.monolithic.application.dto;

import java.util.List;

public record PlaceOrderCommand(
        List<OrderItem> orderItems
) {
    public record OrderItem(
            Long productId,
            Long quantity
    ){}
}
