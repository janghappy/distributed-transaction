package com.example.product.consumer;

import com.example.product.application.ProductService;
import com.example.product.application.dto.ProductBuyCancelCommand;
import com.example.product.application.dto.ProductBuyCommand;
import com.example.product.application.dto.ProductBuyResult;
import com.example.product.consumer.dto.OrderPlacedEvent;
import com.example.product.infrastructure.kafka.QuantityDecreasedFailProducer;
import com.example.product.infrastructure.kafka.QuantityDecreasedProducer;
import com.example.product.infrastructure.kafka.dto.QuantityDecreasedEvent;
import com.example.product.infrastructure.kafka.dto.QuantityDecreasedFailEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPlacedConsumer {

    private final ProductService productService;
    private final QuantityDecreasedProducer quantityDecreasedProducer;
    private final QuantityDecreasedFailProducer quantityDecreasedFailProducer;

    @KafkaListener(
            topics = "order-placed",
            groupId = "order-placed-consumer"
    )
    public void handle(OrderPlacedEvent event) {
        String requestId = event.orderId().toString();

        try {
            ProductBuyResult result = productService.buy(
                    new ProductBuyCommand(
                            requestId,
                            event.productInfos()
                                    .stream()
                                    .map(item -> new ProductBuyCommand.ProductInfo(item.productId(), item.quantity()))
                                    .toList()
                    )
            );

            quantityDecreasedProducer.send(
                    new QuantityDecreasedEvent(
                            event.orderId(),
                            result.totalPrice()
                    )
            );
        } catch (Exception e) {
            productService.cancel(new ProductBuyCancelCommand(requestId));
            quantityDecreasedFailProducer.send(new QuantityDecreasedFailEvent(event.orderId()));
        }
    }
}
