package com.example.point.application;

import com.example.common.LockTransactionExecutor;
import com.example.common.RetryExecutor;
import com.example.point.application.dto.PointReserveCommand;
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

}
