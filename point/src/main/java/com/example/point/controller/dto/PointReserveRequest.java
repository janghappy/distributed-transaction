package com.example.point.controller.dto;

import com.example.point.application.dto.PointReserveCommand;

public record PointReserveRequest(
        String requestId,
        Long userId,
        Long reserveAmount
) {

    public PointReserveCommand toPointReserveCommand() {
        return new PointReserveCommand(requestId, userId, reserveAmount);
    }
}
