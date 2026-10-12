package com.example.order.consumer;

import com.example.event.QuantityDecreasedFailEvent;
import com.example.event.Topics;
import com.example.order.application.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuantityDecreasedFailConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = Topics.QUANTITY_DECREASED_FAIL,
            groupId = "quantity-decreased-fail-consumer"
    )
    public void handle(QuantityDecreasedFailEvent event) {
        orderService.fail(event.orderId());
    }

}
