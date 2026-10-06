package com.example.common;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class RetryExecutor {

    private static final int MAX_TRY_COUNT = 3;
    private static final long RETRY_DELAY_MILLIS = 1000;

    /*
     * 락 획득 실패·낙관적 락 충돌(ConcurrencyFailureException 계열)처럼 잠시 후 다시 하면 성공할 수 있는 경우만 재시도한다.
     * 수량 부족·예약 내역 없음 등 다시 해도 실패할 예외는 잡지 않고 그대로 던진다.
     * 모두 실패하면 마지막 예외를 원인으로 담아 failMessage 예외를 던진다.
     */
    public <T> T execute(Supplier<T> action, String failMessage) {
        int tryCount = 0;
        RuntimeException lastException = null;

        while (tryCount < MAX_TRY_COUNT) {
            try {
                return action.get();
            } catch (ConcurrencyFailureException e) {
                lastException = e;
                tryCount++;
                sleep();
            }
        }

        throw new RuntimeException(failMessage, lastException);
    }

    public void run(Runnable action, String failMessage) {
        execute(() -> {
            action.run();
            return null;
        }, failMessage);
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재시도 대기 중 인터럽트가 발생하였습니다.", e);
        }
    }
}
