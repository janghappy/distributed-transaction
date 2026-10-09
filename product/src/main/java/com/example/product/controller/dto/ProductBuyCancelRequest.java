package com.example.product.controller.dto;

import com.example.product.application.dto.ProductBuyCancelCommand;

public record ProductBuyCancelRequest(String requestId) {
    public ProductBuyCancelCommand toCommand() {
        return new ProductBuyCancelCommand(requestId);
    }
}
