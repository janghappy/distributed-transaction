package com.example.point.application;

import com.example.common.LockTransactionExecutor;
import com.example.common.RetryExecutor;
import com.example.point.application.dto.PointReserveCancelCommand;
import com.example.point.application.dto.PointReserveCommand;
import com.example.point.application.dto.PointReserveConfirmCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointFacadeService {

    private final PointService pointService;
    private final RetryExecutor retryExecutor;
    private final LockTransactionExecutor lockTransactionExecutor;

    public void tryReserve(PointReserveCommand command) {
        retryExecutor.run(
                () -> lockTransactionExecutor.run("point:reserve:" + command.requestId(),
                        () -> pointService.tryReserve(command)),
                "포인트 예약에 실패하였습니다.");
    }

    public void confirmReserve(PointReserveConfirmCommand command) {
        retryExecutor.run(
                () -> lockTransactionExecutor.run("point:confirm:" + command.requestId(),
                        () -> pointService.confirmReserve(command)),
                "포인트 확정에 실패하였습니다.");
    }

    public void cancelReserve(PointReserveCancelCommand command){
        retryExecutor.run(
                () -> lockTransactionExecutor.run("point:cancel:" + command.requestId(),
                        () -> pointService.cancelReserve(command)),
                "포인트 취소에 실패하였습니다.");
    }

}
