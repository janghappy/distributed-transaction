package com.example.order.application;

import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.OrderDto;
import com.example.order.domain.Order;
import com.example.order.domain.OrderItem;
import com.example.order.infrastructure.OrderItemRepository;
import com.example.order.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public long createOrder(CreateOrderCommand command) {
        Order order = orderRepository.save(new Order());
        Long orderId = order.getId();

        List<OrderItem> orderItems = command.orderItems()
                .stream()
                .map(item -> new OrderItem(orderId, item.productId(), item.quantity()))
                .toList();

        orderItemRepository.saveAll(orderItems);
        return orderId;
    }

    @Transactional
    public void reserve(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        order.reserve();
        orderRepository.save(order);
    }

    public OrderDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(orderId);

        return new OrderDto(order.getRequestId(), orderItems.stream()
                .map(item -> new OrderDto.OrderItem(item.getProductId(), item.getQuantity()))
                .toList());
    }

    @Transactional
    public void cancel(Long orderId){
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.cancel();
        orderRepository.save(order);
    }

    @Transactional
    public void confirm(Long orderId){
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.confirm();
        orderRepository.save(order);
    }

    @Transactional
    public void pending(Long orderId){
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.pending();
        orderRepository.save(order);
    }
}
