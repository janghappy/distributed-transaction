package com.example.product.application;

import com.example.product.application.dto.ProductReserveCommand;
import com.example.product.application.dto.ProductReserveResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductFacadeService {

    private static final int MAX_TRY_COUNT = 3;

    private final ProductService productService;

    public ProductReserveResult tryReserve(ProductReserveCommand command) {
        int tryCount = 0;
        RuntimeException lastException = null;

        while (tryCount < MAX_TRY_COUNT) {
            try {
                return productService.tryReserve(command);
            } catch (ConcurrencyFailureException e) {
                // 락 획득 실패·낙관적 락 충돌처럼 잠시 후 다시 하면 성공할 수 있는 경우만 재시도한다.
                // 수량 부족 등 다시 해도 실패할 예외는 잡지 않고 그대로 던진다.
                lastException = e;
                tryCount++;
                sleep();
            }
        }

        throw new RuntimeException("예약에 실패하였습니다.", lastException);
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("예약 재시도 대기 중 인터럽트가 발생하였습니다.", e);
        }
    }
}
