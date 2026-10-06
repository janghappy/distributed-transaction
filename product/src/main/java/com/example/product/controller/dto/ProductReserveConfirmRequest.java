package com.example.product.controller.dto;

import com.example.product.application.dto.ProductReserveConfirmCommand;

public record ProductReserveConfirmRequest(
        String requestId
) {

    public ProductReserveConfirmCommand toProductReserveConfirmCommand() {
        return new ProductReserveConfirmCommand(requestId);
    }
}
