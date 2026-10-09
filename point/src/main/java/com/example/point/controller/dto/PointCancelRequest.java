package com.example.point.controller.dto;

import com.example.point.application.dto.PointCancelCommand;

public record PointCancelRequest(String requestId) {
    public PointCancelCommand toCommand() {
        return new PointCancelCommand(requestId);
    }
}
