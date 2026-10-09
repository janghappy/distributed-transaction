package com.example.product.controller;

import com.example.product.application.ProductService;
import com.example.product.application.RedisLockService;
import com.example.product.application.dto.ProductBuyCancelResult;
import com.example.product.application.dto.ProductBuyResult;
import com.example.product.controller.dto.ProductBuyCancelRequest;
import com.example.product.controller.dto.ProductBuyCancelResponse;
import com.example.product.controller.dto.ProductBuyRequest;
import com.example.product.controller.dto.ProductBuyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final RedisLockService redisLockService;

    @PostMapping("/product/buy")
    public ProductBuyResponse buy(@RequestBody ProductBuyRequest request){
        String lockKey = "product:orchestration:" + request.requestId();

        boolean lockAcquired = redisLockService.tryLock(lockKey, request.requestId());

        if(!lockAcquired){
            System.out.println("락 획득에 실패하였습니다.");
            throw new RuntimeException("락 획득에 실패하였습니다.");
        }

        try {
            ProductBuyResult result = productService.buy(request.toCommand());
            return new ProductBuyResponse(result.totalPrice());
        } finally {
            redisLockService.releaseLock(lockKey);
        }
    }


    @PostMapping("/product/cancel")
    public ProductBuyCancelResponse cancel(@RequestBody ProductBuyCancelRequest request){
        String lockKey = "product:orchestration:" + request.requestId();

        boolean lockAcquired = redisLockService.tryLock(lockKey, request.requestId());

        if(!lockAcquired){
            System.out.println("락 획득에 실패하였습니다.");
            throw new RuntimeException("락 획득에 실패하였습니다.");
        }

        try {
            ProductBuyCancelResult result = productService.cancel(request.toCommand());
            return new ProductBuyCancelResponse(result.totalPrice());
        } finally {
            redisLockService.releaseLock(lockKey);
        }
    }
}
