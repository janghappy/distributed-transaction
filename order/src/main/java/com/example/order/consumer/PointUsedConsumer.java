package com.example.order.consumer;

import com.example.order.application.OrderService;
import com.example.order.consumer.dto.PointUsedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointUsedConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "point-used",
            groupId = "point-used-consumer"
    )
    public void handle(PointUsedEvent event){
        orderService.complete(event.orderId());
    }
}
