package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
// common 모듈의 공통 빈(RedisLockService 등)도 스캔한다.
@SpringBootApplication(scanBasePackages = {"com.example.order", "com.example.common"})
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

}
