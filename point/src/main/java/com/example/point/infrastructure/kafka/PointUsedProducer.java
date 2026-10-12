package com.example.point.infrastructure.kafka;

import com.example.event.PointUsedEvent;
import com.example.event.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointUsedProducer {

    private final KafkaTemplate<String, PointUsedEvent> kafkaTemplate;

    public void send(PointUsedEvent event){
        kafkaTemplate.send(
                Topics.POINT_USED,
                event.orderId().toString(),
                event
        );
    }

}
