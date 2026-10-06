package com.example.product.controller.dto;

import com.example.product.application.dto.ProductReserveCancelCommand;

public record ProductReserveCancelRequest(
        String requestId
) {

    public ProductReserveCancelCommand toProductReserveCancelCommand() {
        return new ProductReserveCancelCommand(requestId);
    }
}
