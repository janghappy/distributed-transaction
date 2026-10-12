package com.example.point.infrastructure.kafka;

import com.example.point.infrastructure.kafka.dto.PointUsedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointUsedProducer {

    private final KafkaTemplate<String, PointUsedEvent> kafkaTemplate;

    public void send(PointUsedEvent event){
        kafkaTemplate.send(
                "point-used",
                event.orderId().toString(),
                event
        );
    }

}
