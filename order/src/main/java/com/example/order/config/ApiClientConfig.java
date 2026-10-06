package com.example.order.config;

import com.example.order.infrastructure.point.PointApiClient;
import com.example.order.infrastructure.product.ProductApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ApiClientConfig {

    @Bean
    public ProductApiClient productApiClient() {
        return new ProductApiClient(RestClient.builder().baseUrl("http://localhost:8081").build());
    }

    @Bean
    public PointApiClient pointApiClient() {
        return new PointApiClient(RestClient.builder().baseUrl("http://localhost:8083").build());
    }
}
