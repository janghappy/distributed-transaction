package com.example.point.controller;

import com.example.point.application.PointFacadeService;
import com.example.point.controller.dto.PointReserveCancelRequest;
import com.example.point.controller.dto.PointReserveConfirmRequest;
import com.example.point.controller.dto.PointReserveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/points")
public class PointController {

    private final PointFacadeService pointFacadeService;

    @PostMapping("/reserve")
    public void reserve(@RequestBody PointReserveRequest request) {
        pointFacadeService.tryReserve(request.toPointReserveCommand());
    }

    @PostMapping("/confirm")
    public void confirm(@RequestBody PointReserveConfirmRequest request) {
        pointFacadeService.confirmReserve(request.toPointReserveConfirmCommand());
    }

    @PostMapping("/cancel")
    public void cancel(@RequestBody PointReserveCancelRequest request) {
        pointFacadeService.cancelReserve(request.toPointReserveCancelCommand());
    }
}
