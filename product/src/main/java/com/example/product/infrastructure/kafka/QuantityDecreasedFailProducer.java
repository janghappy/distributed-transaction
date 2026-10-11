package com.example.product.infrastructure.kafka;

import com.example.product.infrastructure.kafka.dto.QuantityDecreasedFailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuantityDecreasedFailProducer {

    private final KafkaTemplate<String, QuantityDecreasedFailEvent> kafkaTemplate;

    public void send(QuantityDecreasedFailEvent event){
        kafkaTemplate.send(
                "quantity-decreased-fail",
                event.orderId().toString(),
                event
        );
    }
}
