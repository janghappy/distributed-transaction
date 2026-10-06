package com.example.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// common 모듈의 공통 빈(RetryExecutor, LockTransactionExecutor, RedisLockService)도 스캔한다.
@SpringBootApplication(scanBasePackages = {"com.example.product", "com.example.common"})
public class ProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }

}
