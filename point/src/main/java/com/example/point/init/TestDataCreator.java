package com.example.point.init;

import com.example.point.domain.Point;
import com.example.point.infrastructure.PointRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TestDataCreator {

    private final PointRepository pointRepository;

    // ddl-auto: create로 기동할 때마다 테이블이 새로 만들어지므로, 기본 포인트 데이터를 넣어 둔다.
    @PostConstruct
    public void createTestData() {
        pointRepository.save(new Point(1L, 10_000L));
    }
}
