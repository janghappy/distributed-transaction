package com.example.order.infrastructure.kafka;

import com.example.event.OrderPlacedEvent;
import com.example.event.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPlaceProducer {

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void send(OrderPlacedEvent event) {
        kafkaTemplate.send(
                Topics.ORDER_PLACED,
                event.orderId().toString(),
                event
        );
    }
}
