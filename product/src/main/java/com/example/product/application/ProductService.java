package com.example.product.application;

import com.example.product.RedisLockService;
import com.example.product.application.dto.ProductReserveCommand;
import com.example.product.application.dto.ProductReserveResult;
import com.example.product.domain.Product;
import com.example.product.domain.ProductReservation;
import com.example.product.infrastructure.ProductRepository;
import com.example.product.infrastructure.ProductReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductReservationRepository productReservationRepository;
    private final RedisLockService redisLockService;
    private final TransactionTemplate transactionTemplate;

    /*
     * 같은 requestId의 예약 요청이 동시에 들어와도 1번만 처리되도록 Redis 락을 잡는다.
     *
     * 락은 트랜잭션 바깥에서 잡고 푼다. 메서드에 @Transactional을 붙이면 finally의 락 해제가
     * 커밋보다 먼저 실행되어, 그 사이 들어온 요청이 커밋 전 상태(예약 내역 없음)를 읽고 중복 예약할 수 있다.
     */
    protected ProductReserveResult tryReserve(ProductReserveCommand command) {
        String key = "product:reserve:" + command.requestId();

        if (!redisLockService.tryLock(key, command.requestId())) {
            // 동시 요청으로 인한 일시적 실패이므로 재시도 대상 예외(ConcurrencyFailureException 계열)로 던진다.
            throw new CannotAcquireLockException("락 획득에 실패하였습니다.");
        }

        try {
            return transactionTemplate.execute(status -> doTryReserve(command));
        } finally {
            redisLockService.releaseLock(key);
        }
    }

    private ProductReserveResult doTryReserve(ProductReserveCommand command) {
        List<ProductReservation> exists = productReservationRepository.findAllByRequestId(command.requestId());

        if(!exists.isEmpty()) {
            Long totalPrice = exists.stream().mapToLong(ProductReservation::getReservedPrice).sum();

            return new ProductReserveResult(totalPrice);
        }

        Long totalPrice = 0L;
        for (ProductReserveCommand.ReserveItem item : command.items()) {
            Product product = productRepository.findById(item.productId()).orElseThrow();
            Long price = product.reserve(item.reserveQuantity());
            totalPrice += price;

            productRepository.save(product);
            productReservationRepository.save(
                    new ProductReservation(
                            command.requestId(),
                            item.productId(),
                            item.reserveQuantity(),
                            price
                    )
            );
        }

        return new ProductReserveResult(totalPrice);
    }

}
