package com.example.point.application;

import com.example.point.application.dto.PointReserveCommand;
import com.example.point.domain.Point;
import com.example.point.domain.PointReservation;
import com.example.point.infrastructure.PointRepository;
import com.example.point.infrastructure.PointReservationsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointServiceTest {

    @Autowired
    private PointFacadeService pointFacadeService;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private PointReservationsRepository pointReservationsRepository;

    private Point point;

    @BeforeEach
    void setUp() {
        pointReservationsRepository.deleteAllInBatch();
        pointRepository.deleteAllInBatch();

        // userId=1, 포인트 1,000
        point = pointRepository.save(new Point(1L, 1_000L));
    }

    @Test
    void 포인트를_예약하면_예약_금액이_증가하고_예약_내역이_저장된다() {
        // given
        PointReserveCommand command = new PointReserveCommand("request-1", 1L, 50L);

        // when
        pointFacadeService.tryReserve(command);

        // then
        // point 테이블: 실제 포인트는 그대로, 예약 금액만 50 증가
        Point reserved = pointRepository.findByUserId(1L);
        assertThat(reserved.getAmount()).isEqualTo(1_000L);
        assertThat(reserved.getReservedAmount()).isEqualTo(50L);

        // point_reservations 테이블: 예약 내역 1건 저장
        PointReservation reservation = pointReservationsRepository.findByRequestId("request-1");
        assertThat(reservation).isNotNull();
        assertThat(reservation.getPointId()).isEqualTo(point.getId());
        assertThat(reservation.getReservedAmount()).isEqualTo(50L);
        assertThat(reservation.getStatus()).isEqualTo(PointReservation.PointReservationStatus.RESERVED);
        assertThat(pointReservationsRepository.count()).isEqualTo(1);
    }
}
