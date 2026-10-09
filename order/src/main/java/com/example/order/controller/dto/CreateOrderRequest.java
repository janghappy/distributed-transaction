package com.example.order.controller.dto;

import com.example.order.application.dto.CreateOrderCommand;

import java.util.List;

public record CreateOrderRequest(
       List<OrderItem> items
) {
    public record OrderItem(
            Long productId,
            Long quantity
    ) {}

    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(
                items.stream()
                        .map(item -> new CreateOrderCommand.OrderItem(item.productId, item.quantity))
                        .toList()
        );
    }
}
