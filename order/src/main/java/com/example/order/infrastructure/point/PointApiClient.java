package com.example.order.infrastructure.point;

import com.example.order.infrastructure.point.dto.PointReserveApiRequest;
import com.example.order.infrastructure.point.dto.PointReserveCancelApiRequest;
import com.example.order.infrastructure.point.dto.PointReserveConfirmApiRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class PointApiClient {

    private final RestClient restClient;

    public void reserve(PointReserveApiRequest request) {
        restClient.post()
                .uri("/points/reserve")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void confirm(PointReserveConfirmApiRequest request){
        restClient.post()
                .uri("/points/confirm")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void cancel(PointReserveCancelApiRequest request){
        restClient.post()
                .uri("/points/cancel")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

}
