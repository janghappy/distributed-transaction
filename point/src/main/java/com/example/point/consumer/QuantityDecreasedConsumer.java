package com.example.point.consumer;

import com.example.event.PointUseFailEvent;
import com.example.event.PointUsedEvent;
import com.example.event.QuantityDecreasedEvent;
import com.example.event.Topics;
import com.example.point.application.PointService;
import com.example.point.application.dto.PointCancelCommand;
import com.example.point.application.dto.PointUseCommand;
import com.example.point.infrastructure.kafka.PointUseFailProducer;
import com.example.point.infrastructure.kafka.PointUsedProducer;
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
            topics = Topics.QUANTITY_DECREASED,
            groupId = "quantity-decreased-consumer"
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
