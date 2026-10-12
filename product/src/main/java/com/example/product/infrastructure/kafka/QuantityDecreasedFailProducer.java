package com.example.product.infrastructure.kafka;

import com.example.event.QuantityDecreasedFailEvent;
import com.example.event.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuantityDecreasedFailProducer {

    private final KafkaTemplate<String, QuantityDecreasedFailEvent> kafkaTemplate;

    public void send(QuantityDecreasedFailEvent event){
        kafkaTemplate.send(
                Topics.QUANTITY_DECREASED_FAIL,
                event.orderId().toString(),
                event
        );
    }
}
