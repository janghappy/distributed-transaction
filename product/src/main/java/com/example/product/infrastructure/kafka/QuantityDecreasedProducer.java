package com.example.product.infrastructure.kafka;

import com.example.event.QuantityDecreasedEvent;
import com.example.event.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuantityDecreasedProducer {

    private final KafkaTemplate<String, QuantityDecreasedEvent> kafkaTemplate;

    public void send(QuantityDecreasedEvent event){
        kafkaTemplate.send(
                Topics.QUANTITY_DECREASED,
                event.orderId().toString(),
                event
        );
    }
}
