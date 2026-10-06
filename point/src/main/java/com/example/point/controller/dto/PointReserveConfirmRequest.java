package com.example.point.controller.dto;

import com.example.point.application.dto.PointReserveConfirmCommand;

public record PointReserveConfirmRequest(
        String requestId
) {

    public PointReserveConfirmCommand toPointReserveConfirmCommand() {
        return new PointReserveConfirmCommand(requestId);
    }
}
