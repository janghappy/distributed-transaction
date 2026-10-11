package com.example.order.controller;

import com.example.order.application.OrderCoordinator;
import com.example.order.application.OrderService;
import com.example.order.application.RedisLockService;
import com.example.order.application.dto.CreateOrderResult;
import com.example.order.application.dto.PlaceOrderCommand;
import com.example.order.controller.dto.CreateOrderRequest;
import com.example.order.controller.dto.CreateOrderResponse;
import com.example.order.controller.dto.PlaceOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderCoordinator orderCoordinator;
    private final RedisLockService redisLockService;

    @PostMapping("/order")
    public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        CreateOrderResult result = orderService.createOrder(request.toCommand());

        return new CreateOrderResponse(result.orderId());
    }

    @PostMapping("/order/place")
    public void placeOrder(@RequestBody PlaceOrderRequest request) {
        String key = "order:orchestration:" + request.orderId();

        if (!redisLockService.tryLock(key, request.orderId().toString())) {
            throw new RuntimeException("락 획득에 실패하였습니다.");
        }

        try {
            orderCoordinator.placeOrder(request.toCommand());
        } finally {
            redisLockService.releaseLock(key);
        }
    }
}
