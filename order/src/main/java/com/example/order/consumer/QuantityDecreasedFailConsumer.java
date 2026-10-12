package com.example.order.consumer;

import com.example.order.application.OrderService;
import com.example.order.consumer.dto.QuantityDecreasedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuantityDecreasedFailConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "quantity-decreased-fail",
            groupId = "quantity-decreased-fail-consumer",
            properties = {
                    "spring.json.value.default.type = com.example.order.consumer.dto.QuantityDecreasedEvent"
            }
    )
    public void handle(QuantityDecreasedEvent event) {
        orderService.fail(event.orderId());
    }

}
