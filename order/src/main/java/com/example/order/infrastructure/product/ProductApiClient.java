package com.example.order.infrastructure.product;

import com.example.order.infrastructure.product.dto.ProductReserveApiRequest;
import com.example.order.infrastructure.product.dto.ProductReserveApiResponse;
import com.example.order.infrastructure.product.dto.ProductReserveCancelApiRequest;
import com.example.order.infrastructure.product.dto.ProductReserveConfirmApiRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class ProductApiClient {

    private final RestClient restClient;

    public ProductReserveApiResponse reserve(ProductReserveApiRequest request) {
        return restClient.post()
                .uri("/products/reserve")
                .body(request)
                .retrieve()
                .body(ProductReserveApiResponse.class);
    }

    public void confirm(ProductReserveConfirmApiRequest request) {
        restClient.post()
                .uri("/products/confirm")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void cancel(ProductReserveCancelApiRequest request) {
        restClient.post()
                .uri("/products/cancel")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

}
