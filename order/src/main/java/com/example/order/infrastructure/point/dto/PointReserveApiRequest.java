package com.example.order.infrastructure.point.dto;

public record PointReserveApiRequest(
        String requestId,
        Long userId,
        Long reserveAmount
) { }
