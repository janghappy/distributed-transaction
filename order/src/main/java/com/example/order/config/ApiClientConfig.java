package com.example.order.config;

import com.example.order.infrastructure.point.PointApiClient;
import com.example.order.infrastructure.product.ProductApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ApiClientConfig {

    @Bean
    public ProductApiClient productApiClient() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(2));

        return new ProductApiClient(
                RestClient.builder()
                        .requestFactory(factory)
                        .baseUrl("http://localhost:8081")
                        .build()
        );
    }

    @Bean
    public PointApiClient pointApiClient() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(2));

        return new PointApiClient(
                RestClient.builder()
                        .requestFactory(factory)
                        .baseUrl("http://localhost:8083")
                        .build()
        );
    }
}
