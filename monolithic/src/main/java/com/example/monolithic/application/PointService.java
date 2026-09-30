package com.example.monolithic.application;

import com.example.monolithic.infrastructure.PointRepository;
import com.example.monolithic.product.domain.Point;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PointService {

    private final PointRepository pointRepository;

    @Transactional
    public void use(Long userId, Long amount) {
        Point point = pointRepository.findByUserId(userId);

        /*
        // 임시 - 강제 트랜잭션 내 장애 발생
        if(true){
            throw new RuntimeException("foo");
        }
        */

        if (point == null) {
            throw new RuntimeException("포인트가 존재하지 않습니다.");
        }

        point.use(amount);
        pointRepository.save(point);
    }

}
