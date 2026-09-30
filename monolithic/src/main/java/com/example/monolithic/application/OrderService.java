package com.example.monolithic.application;

import com.example.monolithic.application.dto.PlaceOrderCommand;
import com.example.monolithic.infrastructure.OrderItemRepository;
import com.example.monolithic.infrastructure.OrderRepository;
import com.example.monolithic.product.domain.Order;
import com.example.monolithic.product.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrderService  {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PointService pointService;
    private final ProductService productService;

    // 주문 저장, 재고 차감, 포인트 사용을 하나의 트랜잭션으로 묶어 정합성을 보장한다.
    // 재고·포인트 부족 등으로 중간에 예외가 발생하면 전체가 롤백된다.
    @Transactional
    public void placeOrder(PlaceOrderCommand command) {
        Order order = orderRepository.save(new Order());
        long totalPrice = 0L;

        for (PlaceOrderCommand.OrderItem item : command.orderItems()) {
            OrderItem orderItem = new OrderItem(order.getId(), item.productId(), item.quantity());
            orderItemRepository.save(orderItem);

            long price = productService.buy(item.productId(), item.quantity());
            totalPrice += price;
        }

        pointService.use(1L, totalPrice);
    }
}
