package com.example.common;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@RequiredArgsConstructor
@Component
public class LockTransactionExecutor {

    private final RedisLockService redisLockService;
    private final TransactionTemplate transactionTemplate;

    /*
     * 같은 key의 요청이 동시에 들어와도 1번만 처리되도록 Redis 락을 잡고, 락 안에서 트랜잭션을 실행한다.
     *
     * 락은 트랜잭션 바깥에서 잡고 푼다. 메서드에 @Transactional을 붙이면 finally의 락 해제가
     * 커밋보다 먼저 실행되어, 그 사이 들어온 요청이 커밋 전 상태를 읽고 중복 처리할 수 있다.
     */
    public <T> T execute(String key, Supplier<T> action) {
        if (!redisLockService.tryLock(key, key)) {
            // 동시 요청으로 인한 일시적 실패이므로 재시도 대상 예외(ConcurrencyFailureException 계열)로 던진다.
            throw new CannotAcquireLockException("락 획득에 실패하였습니다.");
        }

        try {
            return transactionTemplate.execute(status -> action.get());
        } finally {
            redisLockService.releaseLock(key);
        }
    }

    public void run(String key, Runnable action) {
        execute(key, () -> {
            action.run();
            return null;
        });
    }
}
