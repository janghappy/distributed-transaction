package com.example.point.infrastructure.kafka;

import com.example.event.PointUseFailEvent;
import com.example.event.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointUseFailProducer {

    private final KafkaTemplate<String, PointUseFailEvent> kafkaTemplate;

    public void send(PointUseFailEvent event){
        kafkaTemplate.send(
                Topics.POINT_USE_FAIL,
                event.orderId().toString(),
                event
        );
    }

}
