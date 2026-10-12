package com.example.order.consumer;

import com.example.event.PointUsedEvent;
import com.example.event.Topics;
import com.example.order.application.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointUsedConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = Topics.POINT_USED,
            groupId = "point-used-consumer"
    )
    public void handle(PointUsedEvent event){
        orderService.complete(event.orderId());
    }
}
