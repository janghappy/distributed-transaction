package com.example.product.application;

import com.example.common.LockTransactionExecutor;
import com.example.common.RetryExecutor;
import com.example.product.application.dto.ProductReserveCancelCommand;
import com.example.product.application.dto.ProductReserveCommand;
import com.example.product.application.dto.ProductReserveConfirmCommand;
import com.example.product.application.dto.ProductReserveResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductFacadeService {

    private final ProductService productService;
    private final RetryExecutor retryExecutor;
    private final LockTransactionExecutor lockTransactionExecutor;

    public ProductReserveResult tryReserve(ProductReserveCommand command) {
        return retryExecutor.execute(
                () -> lockTransactionExecutor.execute("product:reserve:" + command.requestId(),
                        () -> productService.tryReserve(command)),
                "예약에 실패하였습니다.");
    }

    public void confirmReserve(ProductReserveConfirmCommand command) {
        retryExecutor.run(
                () -> lockTransactionExecutor.run("product:confirm:" + command.requestId(),
                        () -> productService.confirmReserve(command)),
                "예약 확정에 실패하였습니다.");
    }

    public void cancelResolved(ProductReserveCancelCommand command) {
        retryExecutor.run(
                () -> lockTransactionExecutor.run("product:cancel:" + command.requestId(),
                        () -> productService.cancelResolved(command)),
                "예약 취소에 실패하였습니다.");
    }
}
