package com.example.point.controller;

import com.example.point.application.PointService;
import com.example.point.application.RedisLockService;
import com.example.point.application.dto.PointCancelCommand;
import com.example.point.controller.dto.PointCancelRequest;
import com.example.point.controller.dto.PointUseRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final RedisLockService redisLockService;

    @PostMapping("/point/use")
    public void use(@RequestBody @Valid PointUseRequest request) {
        String lockKey = "point:orchestration:" + request.requestId();

        boolean lockAcquired = redisLockService.tryLock(lockKey, request.requestId());

        if (!lockAcquired) {
            throw new RuntimeException("락 획득에 실패하였습니다.");
        }

        try {
            pointService.use(request.toCommand());
        } finally {
            redisLockService.releaseLock(lockKey);
        }
    }

    @PostMapping("/point/cancel")
    public void cancel(@RequestBody @Valid PointCancelRequest request) {
        String lockKey = "point:orchestration:" + request.requestId();

        boolean lockAcquired = redisLockService.tryLock(lockKey, request.requestId());

        if (!lockAcquired) {
            throw new RuntimeException("락 획득에 실패하였습니다.");
        }

        try {
            pointService.cancel(request.toCommand());
        } finally {
            redisLockService.releaseLock(lockKey);
        }
    }
}
