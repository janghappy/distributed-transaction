package com.example.product.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class RedisLockService {

    // 락을 잡은 서버가 해제 전에 죽어도 락이 영구히 남지 않도록 만료 시간을 둔다.
    // 주문 처리 시간보다 충분히 길어야, 처리 도중 만료되어 다른 요청이 락을 잡는 일이 없다.
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final StringRedisTemplate stringRedisTemplate;

    // setIfAbsent는 파이프라인/트랜잭션 모드에서 null을 반환할 수 있어, null이면 실패로 본다.
    public boolean tryLock(String key, String value) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, value, LOCK_TTL));
    }

    public void releaseLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
