package com.example.product.consumer;

import com.example.event.OrderPlacedEvent;
import com.example.event.QuantityDecreasedEvent;
import com.example.event.QuantityDecreasedFailEvent;
import com.example.event.Topics;
import com.example.product.application.ProductService;
import com.example.product.application.dto.ProductBuyCancelCommand;
import com.example.product.application.dto.ProductBuyCommand;
import com.example.product.application.dto.ProductBuyResult;
import com.example.product.infrastructure.kafka.QuantityDecreasedFailProducer;
import com.example.product.infrastructure.kafka.QuantityDecreasedProducer;
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
            topics = Topics.ORDER_PLACED,
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
