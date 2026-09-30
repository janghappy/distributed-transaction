package com.example.monolithic.application;

import com.example.monolithic.RedisLockService;
import com.example.monolithic.application.dto.CreateOrderCommand;
import com.example.monolithic.application.dto.PlaceOrderCommand;
import com.example.monolithic.infrastructure.OrderItemRepository;
import com.example.monolithic.infrastructure.OrderRepository;
import com.example.monolithic.product.domain.Order;
import com.example.monolithic.product.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PointService pointService;
    private final ProductService productService;
    private final RedisLockService redisLockService;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public long createOrder(CreateOrderCommand command) {
        Order order = orderRepository.save(new Order());
        Long orderId = order.getId();

        List<OrderItem> orderItems = command.orderItems().stream()
                .map(item -> new OrderItem(orderId, item.productId(), item.quantity()))
                .toList();

        orderItemRepository.saveAll(orderItems);
        return orderId;
    }

    /*
     * 주문 저장, 재고 차감, 포인트 사용을 하나의 트랜잭션으로 묶어 정합성을 보장한다.
     * 재고·포인트 부족 등으로 중간에 예외가 발생하면 전체가 롤백된다.
     *
     * 락은 트랜잭션 바깥에서 잡고 푼다. 메서드에 @Transactional을 붙이면 finally의 락 해제가
     * 커밋보다 먼저 실행되어, 그 사이 들어온 요청이 커밋 전 상태(COMPLETED 아님)를 읽고 중복 처리할 수 있다.
     */
    public void placeOrder(long orderId) {
        String key = "order:monolithic:" + orderId;

        if (!redisLockService.tryLock(key, orderId + "")) {
            throw new RuntimeException("락 획득에 실패하였습니다.");
        }

        try {
            transactionTemplate.executeWithoutResult(status -> doPlaceOrder(orderId));
        } finally {
            redisLockService.releaseLock(key);
        }
    }

    private void doPlaceOrder(long orderId) {
        long totalPrice = 0L;
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("주문정보가 존재하지 않습니다."));

        // 같은 주문이 2번 요청되어도 재고·포인트가 중복 차감되지 않도록,
        // 이미 완료된 주문이면 처리하지 않고 반환한다.
        if (order.getStatus() == Order.OrderStatus.COMPLETED) {
            return;
        }

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        for (OrderItem item : orderItems) {
            long price = productService.buy(item.getProductId(), item.getQuantity());
            totalPrice += price;
        }

        pointService.use(1L, totalPrice);

        order.complete();
        orderRepository.save(order);

        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
