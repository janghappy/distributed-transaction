package com.example.order.application;

import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.CreateOrderResult;
import com.example.order.application.dto.OrderDto;
import com.example.order.application.dto.PlaceOrderCommand;
import com.example.order.domain.Order;
import com.example.order.domain.OrderItem;
import com.example.order.infrastructure.OrderItemRepository;
import com.example.order.infrastructure.OrderRepository;
import com.example.order.infrastructure.kafka.OrderPlaceProducer;
import com.example.order.infrastructure.kafka.dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPlaceProducer orderPlaceProducer;

    @Transactional
    public CreateOrderResult createOrder(CreateOrderCommand command) {
        Order order = orderRepository.save(new Order());

        List<OrderItem> orderItems = command.items()
                .stream()
                .map(item -> new OrderItem(order.getId(), item.productId(), item.quantity()))
                .toList();

        orderItemRepository.saveAll(orderItems);

        return new CreateOrderResult(order.getId());
    }

    @Transactional
    public void placeOrder(PlaceOrderCommand command) {
        Order order = orderRepository.findById(command.orderId()).orElseThrow();
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());

        order.request();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                orderPlaceProducer.send(
                        new OrderPlacedEvent(
                                command.orderId(),
                                orderItems.stream()
                                        .map(item -> new OrderPlacedEvent.ProductInfo(item.getProductId(), item.getQuantity()))
                                        .toList()
                        )
                );
            }
        });
    }

    public OrderDto getOrder(Long orderId) {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId);

        return new OrderDto(
                orderItems.stream()
                        .map((OrderItem item) -> new OrderDto.OrderItem(item.getProductId(), item.getQuantity()))
                        .toList()
        );
    }

    @Transactional
    public void request(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        order.request();
    }

    @Transactional
    public void complete(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        order.complete();
    }

    @Transactional
    public void fail(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.fail();
    }
}
