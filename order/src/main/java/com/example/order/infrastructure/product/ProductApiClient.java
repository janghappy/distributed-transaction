package com.example.order.infrastructure.product;

import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class ProductApiClient {

    private final RestClient restClient;

    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    public ProductBuyApiResponse buy(ProductBuyApiRequest request) {
        return restClient
                .post()
                .uri("/product/buy")
                .body(request)
                .retrieve()
                .body(ProductBuyApiResponse.class);
    }

    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    public ProductBuyCancelApiResponse cancel(ProductBuyCancelApiRequest request) {
        return restClient
                .post()
                .uri("/product/buy/cancel")
                .body(request)
                .retrieve()
                .body(ProductBuyCancelApiResponse.class);
    }

}
