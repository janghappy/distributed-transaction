package com.example.point.infrastructure.kafka;

import com.example.point.infrastructure.kafka.dto.PointUseFailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointUseFailProducer {

    private final KafkaTemplate<String, PointUseFailEvent> kafkaTemplate;

    public void send(PointUseFailEvent event){
        kafkaTemplate.send(
                "point-use-fail",
                event.orderId().toString(),
                event
        );
    }

}
