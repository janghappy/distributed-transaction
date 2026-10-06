package com.example.order.controller;

import com.example.common.RedisLockService;
import com.example.order.application.OrderCoordinator;
import com.example.order.application.OrderService;
import com.example.order.controller.dto.CreateOrderRequest;
import com.example.order.controller.dto.CreateOrderResponse;
import com.example.order.controller.dto.PlaceOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class OrderController {

    private final OrderService orderService;
    private final OrderCoordinator orderCoordinator;
    private final RedisLockService redisLockService;

    @PostMapping("/order")
    public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        long orderId = orderService.createOrder(request.toCreateOrderCommand());

        return new CreateOrderResponse(orderId);
    }

    @PostMapping("/order/place")
    public void placeOrder(@RequestBody PlaceOrderRequest request) {
        // 같은 주문이 동시에 처리되지 않도록 Redis 락을 잡는다.
        String key = "order:place:" + request.orderId();
        boolean acquiredLock = redisLockService.tryLock(key, request.orderId().toString());

        if (!acquiredLock) {
            throw new CannotAcquireLockException("락 획득에 실패하였습니다.");
        }

        // 성공·실패와 관계없이 락을 해제하고, 실패 예외는 그대로 전달한다.
        try {
            orderCoordinator.placeOrder(request.toCommand());
        } finally {
            redisLockService.releaseLock(key);
        }
    }
}
