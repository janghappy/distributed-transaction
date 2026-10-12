package com.example.order.controller;

import com.example.order.application.OrderService;
import com.example.order.application.RedisLockService;
import com.example.order.application.dto.CreateOrderResult;
import com.example.order.controller.dto.CreateOrderRequest;
import com.example.order.controller.dto.CreateOrderResponse;
import com.example.order.controller.dto.PlaceOrderRequest;
import com.example.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
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
            orderService.placeOrder(request.toCommand());
        } finally {
            redisLockService.releaseLock(key);
        }
    }

    @GetMapping("/order/{id}/status")
    public Order.OrderStatus getOrderStatus(@PathVariable Long id) {
        return orderService.getStatus(id);
    }
}
