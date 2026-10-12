package com.example.point.consumer;

import com.example.point.application.PointService;
import com.example.point.application.dto.PointCancelCommand;
import com.example.point.application.dto.PointUseCommand;
import com.example.point.consumer.dto.QuantityDecreasedEvent;
import com.example.point.infrastructure.kafka.PointUsedProducer;
import com.example.point.infrastructure.kafka.PointUseFailProducer;
import com.example.point.infrastructure.kafka.dto.PointUseFailEvent;
import com.example.point.infrastructure.kafka.dto.PointUsedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuantityDecreasedConsumer {

    private final PointService pointService;
    private final PointUsedProducer pointUsedProducer;
    private final PointUseFailProducer pointUseFailProducer;

    @KafkaListener(
            topics = "quantity-decreased",
            groupId = "quantity-decreased-consumer",
            properties = {
                    "spring.json.value.default.type = com.example.point.consumer.dto.QuantityDecreasedEvent"
            }
    )
    public void handle(QuantityDecreasedEvent event){
        String requestId = event.orderId().toString();
        try {
            pointService.use(new PointUseCommand(requestId, 1L, event.totalPrice()));

            pointUsedProducer.send(new PointUsedEvent(event.orderId()));
        } catch (Exception e) {
            pointService.cancel(new PointCancelCommand(requestId));
            pointUseFailProducer.send(new PointUseFailEvent(event.orderId()));
        }
    }

}
