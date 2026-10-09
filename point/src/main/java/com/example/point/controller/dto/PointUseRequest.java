package com.example.point.controller.dto;

import com.example.point.application.dto.PointUseCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PointUseRequest (
        @NotBlank String requestId,
        @NotNull Long userId,
        @NotNull Long amount
){
    public PointUseCommand toCommand(){
        return new PointUseCommand(requestId, userId, amount);
    }
}
