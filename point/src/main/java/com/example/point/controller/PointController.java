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
@RequestMapping()
public class PointController {

    private final PointFacadeService pointFacadeService;

    int count = 0;

    @PostMapping("/points/reserve")
    public void reserve(@RequestBody PointReserveRequest request) throws InterruptedException {
        System.out.println("진입!!");
        if(count%2==0){
            count++;
            Thread.sleep(2000);
        }

        pointFacadeService.tryReserve(request.toPointReserveCommand());
    }

    @PostMapping("/points/confirm")
    public void confirm(@RequestBody PointReserveConfirmRequest request) {
        pointFacadeService.confirmReserve(request.toPointReserveConfirmCommand());
    }

    @PostMapping("/points/cancel")
    public void cancel(@RequestBody PointReserveCancelRequest request) {
        pointFacadeService.cancelReserve(request.toPointReserveCancelCommand());
    }
}
