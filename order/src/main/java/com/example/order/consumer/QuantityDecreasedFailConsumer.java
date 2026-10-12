package com.example.order.consumer;

import com.example.order.application.OrderService;
import com.example.order.consumer.dto.QuantityDecreasedFailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuantityDecreasedFailConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "quantity-decreased-fail",
            groupId = "quantity-decreased-fail-consumer"
    )
    public void handle(QuantityDecreasedFailEvent event) {
        orderService.fail(event.orderId());
    }

}
