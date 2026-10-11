package com.example.order.infrastructure.kafka;

import com.example.order.infrastructure.kafka.dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPlaceProducer {

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void send(OrderPlacedEvent event) {
        kafkaTemplate.send(
                "order-placed",
                event.orderId().toString(),
                event
        );
    }
}
