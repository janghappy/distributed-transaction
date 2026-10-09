package com.example.product.controller.dto;

import com.example.product.application.dto.ProductBuyCommand;

import java.util.List;

public record ProductBuyRequest(
        String requestId,
        List<ProductInfo> productInfos
) {

    public ProductBuyCommand toCommand() {
        return new ProductBuyCommand(
                requestId,
                productInfos.stream()
                        .map(info -> new ProductBuyCommand.ProductInfo(info.productId, info.quantity))
                        .toList()
        );
    }

    public record ProductInfo(
            Long productId,
            Long quantity
    ) {}
}
