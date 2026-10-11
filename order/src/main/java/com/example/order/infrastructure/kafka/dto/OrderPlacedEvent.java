package com.example.order.infrastructure.kafka.dto;

import java.util.List;

public record OrderPlacedEvent (
        Long orderId,
        List<ProductInfo> productInfos
){
    public record ProductInfo(
            Long productId,
            Long quantity
    ){}
}
