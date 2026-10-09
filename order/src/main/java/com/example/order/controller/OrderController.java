package com.example.order.controller;

import com.example.order.application.OrderService;
import com.example.order.application.dto.CreateOrderResult;
import com.example.order.controller.dto.CreateOrderRequest;
import com.example.order.controller.dto.CreateOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/order")
    public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        CreateOrderResult result = orderService.createOrder(request.toCommand());

        return new CreateOrderResponse(result.orderId());
    }
}
