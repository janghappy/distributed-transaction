//package com.example.monolithic.application;
//
//import com.example.monolithic.application.dto.PlaceOrderCommand;
//import com.example.monolithic.infrastructure.OrderItemRepository;
//import com.example.monolithic.infrastructure.OrderRepository;
//import com.example.monolithic.infrastructure.PointRepository;
//import com.example.monolithic.infrastructure.ProductRepository;
//import com.example.monolithic.product.domain.Point;
//import com.example.monolithic.product.domain.Product;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//class OrderServiceTest {
//
//    @Autowired
//    private OrderService orderService;
//
//    @Autowired
//    private OrderRepository orderRepository;
//
//    @Autowired
//    private OrderItemRepository orderItemRepository;
//
//    @Autowired
//    private ProductRepository productRepository;
//
//    @Autowired
//    private PointRepository pointRepository;
//
//    private Product product1;
//    private Product product2;
//
//    @BeforeEach
//    void setUp() {
//        orderItemRepository.deleteAllInBatch();
//        orderRepository.deleteAllInBatch();
//        productRepository.deleteAllInBatch();
//        pointRepository.deleteAllInBatch();
//
//        pointRepository.save(new Point(1L, 10_000L));
//        product1 = productRepository.save(new Product(100L, 100L));
//        product2 = productRepository.save(new Product(200L, 200L));
//    }
//
//    @Test
//    void 주문을_하면_재고와_포인트가_차감된다() {
//        // given
//        PlaceOrderCommand command = new PlaceOrderCommand(List.of(
//                new PlaceOrderCommand.OrderItem(product1.getId(), 1L),
//                new PlaceOrderCommand.OrderItem(product2.getId(), 2L)
//        ));
//
//        // when
//        orderService.placeOrder(command);
//
//        // then
//        assertThat(orderRepository.count()).isEqualTo(1);
//        assertThat(orderItemRepository.count()).isEqualTo(2);
//
//        assertThat(productRepository.findById(product1.getId()).orElseThrow().getQuantity()).isEqualTo(99L);
//        assertThat(productRepository.findById(product2.getId()).orElseThrow().getQuantity()).isEqualTo(198L);
//
//        // 100 * 1 + 200 * 2 = 500
//        assertThat(pointRepository.findByUserId(1L).getAmount()).isEqualTo(9_500L);
//    }
//}
