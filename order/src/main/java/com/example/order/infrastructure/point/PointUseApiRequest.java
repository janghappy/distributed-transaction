package com.example.order.infrastructure.point;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PointUseApiRequest(
        @NotBlank String requestId,
        @NotNull Long userId,
        @NotNull Long amount
) {
}
