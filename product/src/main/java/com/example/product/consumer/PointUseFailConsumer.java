package com.example.product.consumer;

import com.example.event.PointUseFailEvent;
import com.example.event.QuantityDecreasedFailEvent;
import com.example.event.Topics;
import com.example.product.application.ProductService;
import com.example.product.application.dto.ProductBuyCancelCommand;
import com.example.product.infrastructure.kafka.QuantityDecreasedFailProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointUseFailConsumer {

    private final ProductService productService;
    private final QuantityDecreasedFailProducer quantityDecreasedFailProducer;

    @KafkaListener(
            topics = Topics.POINT_USE_FAIL,
            groupId = "point-use-fail-consumer"
    )
    public void handle(PointUseFailEvent event) {
        productService.cancel(new ProductBuyCancelCommand(event.orderId().toString()));
        quantityDecreasedFailProducer.send(new QuantityDecreasedFailEvent(event.orderId()));
    }
}
