package com.example.monolithic.application;

import com.example.monolithic.application.dto.CreateOrderCommand;
import com.example.monolithic.infrastructure.OrderItemRepository;
import com.example.monolithic.infrastructure.OrderRepository;
import com.example.monolithic.infrastructure.PointRepository;
import com.example.monolithic.infrastructure.ProductRepository;
import com.example.monolithic.product.domain.Order;
import com.example.monolithic.product.domain.Point;
import com.example.monolithic.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderRollbackTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointRepository pointRepository;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        pointRepository.deleteAllInBatch();

        pointRepository.save(new Point(1L, 10_000L));
        product1 = productRepository.save(new Product(100L, 100L));
        product2 = productRepository.save(new Product(200L, 200L));
    }

    /*
     * 재고 차감 후 포인트 사용에서 실패하면, 이미 차감한 재고까지 전부 롤백되는지 검증한다.
     * 재고 차감과 포인트 사용이 TransactionTemplate 하나의 트랜잭션으로 묶여 있기 때문이다.
     */
    @Test
    void 포인트가_부족하면_재고_차감도_롤백된다() {
        // given: 재고는 충분하지만 주문 금액 (100 * 1) + (200 * 60) = 12,100 이 포인트 10,000 보다 크다.
        CreateOrderCommand command = new CreateOrderCommand(List.of(
                new CreateOrderCommand.OrderItem(product1.getId(), 1L),
                new CreateOrderCommand.OrderItem(product2.getId(), 60L)
        ));
        long orderId = orderService.createOrder(command);

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(orderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("잔액이 부족합니다.");

        assertThat(productRepository.findById(product1.getId()).orElseThrow().getQuantity()).isEqualTo(100L);
        assertThat(productRepository.findById(product2.getId()).orElseThrow().getQuantity()).isEqualTo(200L);
        assertThat(pointRepository.findByUserId(1L).getAmount()).isEqualTo(10_000L);
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(Order.OrderStatus.CREATED);
    }
}
