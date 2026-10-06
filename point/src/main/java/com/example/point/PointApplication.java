package com.example.point;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// common 모듈의 공통 빈(RetryExecutor, LockTransactionExecutor, RedisLockService)도 스캔한다.
@SpringBootApplication(scanBasePackages = {"com.example.point", "com.example.common"})
public class PointApplication {

    public static void main(String[] args) {
        SpringApplication.run(PointApplication.class, args);
    }

}
