package com.example.monolithic.application;

import com.example.monolithic.application.dto.CreateOrderCommand;
import com.example.monolithic.infrastructure.OrderItemRepository;
import com.example.monolithic.infrastructure.OrderRepository;
import com.example.monolithic.infrastructure.PointRepository;
import com.example.monolithic.infrastructure.ProductRepository;
import com.example.monolithic.product.domain.Point;
import com.example.monolithic.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DuplicateOrderTest {

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
     * 같은 주문(orderId)을 2번 요청해도 1번만 처리되어 재고·포인트가 1번만 차감되는지 검증한다.
     */
    @Test
    void 동일한_주문을_2번_요청해도_1번만_처리된다() {
        // given
        CreateOrderCommand command = new CreateOrderCommand(List.of(
                new CreateOrderCommand.OrderItem(product1.getId(), 1L),
                new CreateOrderCommand.OrderItem(product2.getId(), 2L)
        ));
        long orderId = orderService.createOrder(command);

        // when
        orderService.placeOrder(orderId);
        orderService.placeOrder(orderId);

        // then
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(orderItemRepository.count()).isEqualTo(2);

        assertThat(productRepository.findById(product1.getId()).orElseThrow().getQuantity()).isEqualTo(99L);
        assertThat(productRepository.findById(product2.getId()).orElseThrow().getQuantity()).isEqualTo(198L);

        // (100 * 1) + (200 * 2) = 500
        assertThat(pointRepository.findByUserId(1L).getAmount()).isEqualTo(9_500L);
    }

    /*
     * 같은 주문이 동시에 여러 번 들어와도 Redis 락으로 1번만 처리되는지 검증한다.
     * 락을 먼저 잡은 요청만 처리되고, 나머지는 락 획득에 실패해 예외로 거절된다.
     */
    @Test
    void 동일한_주문을_동시에_요청해도_1번만_처리된다() throws InterruptedException {
        // given
        CreateOrderCommand command = new CreateOrderCommand(List.of(
                new CreateOrderCommand.OrderItem(product1.getId(), 1L),
                new CreateOrderCommand.OrderItem(product2.getId(), 2L)
        ));
        long orderId = orderService.createOrder(command);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        Queue<String> failMessages = new ConcurrentLinkedQueue<>();

        // when: 모든 스레드가 준비된 뒤 한꺼번에 출발시켜 요청이 겹치게 한다.
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    orderService.placeOrder(orderId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failMessages.add(e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        // then
        failMessages.forEach(message -> System.out.println("주문 처리 실패: " + message));

        // 실패한 요청은 모두 락 획득 실패로 거절되어야 한다. (재고·포인트 부족 등 다른 이유로 실패하면 안 된다)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failMessages).hasSize(threadCount - 1)
                .allMatch(message -> message.equals("락 획득에 실패하였습니다."));

        assertThat(productRepository.findById(product1.getId()).orElseThrow().getQuantity()).isEqualTo(99L);
        assertThat(productRepository.findById(product2.getId()).orElseThrow().getQuantity()).isEqualTo(198L);
        assertThat(pointRepository.findByUserId(1L).getAmount()).isEqualTo(9_500L);
    }
}
