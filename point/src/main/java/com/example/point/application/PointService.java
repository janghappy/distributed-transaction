package com.example.point.application;

import com.example.point.application.dto.PointReserveCancelCommand;
import com.example.point.application.dto.PointReserveCommand;
import com.example.point.application.dto.PointReserveConfirmCommand;
import com.example.point.domain.Point;
import com.example.point.domain.PointReservation;
import com.example.point.infrastructure.PointRepository;
import com.example.point.infrastructure.PointReservationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointReservationsRepository pointReservationsRepository;

    // 락·트랜잭션은 PointFacadeService에서 감싸므로, Facade를 통해서만 호출되도록 protected로 둔다.
    protected void tryReserve(PointReserveCommand command){
        PointReservation reservation = pointReservationsRepository.findByRequestId(command.requestId());

        if(reservation != null){
            System.out.println("이미 예약된 요청입니다.");
            return;
        }

        Point point = pointRepository.findByUserId(command.userId());
        point.reserve(command.reserveAmount());
        pointReservationsRepository.save(
                new PointReservation(
                        command.requestId(),
                        point.getId(),
                        command.reserveAmount()
                )
        );
    }

    protected void confirmReserve(PointReserveConfirmCommand command) {
        PointReservation reservation = pointReservationsRepository.findByRequestId(command.requestId());

        if(reservation == null){
            throw new RuntimeException("예약내역이 없습니다.");
        }

        if(reservation.getStatus() == PointReservation.PointReservationStatus.CONFIRMED){
            System.out.println("이미 확정된 예약입니다.");
            return;
        }

        Point point = pointRepository.findById(reservation.getPointId()).orElseThrow();
        point.confirm(reservation.getReservedAmount());

        reservation.confirm();

        pointRepository.save(point);
        pointReservationsRepository.save(reservation);
    }

    protected void cancelReserve(PointReserveCancelCommand command) {
        PointReservation reservation = pointReservationsRepository.findByRequestId(command.requestId());

        if(reservation == null){
            throw new RuntimeException("예약내역이 존재하지 않습니다.");
        }

        if(reservation.getStatus() == PointReservation.PointReservationStatus.CANCELLED){
            throw new RuntimeException("이미 취소된 예약입니다.");
        }

        Point point = pointRepository.findById(reservation.getPointId()).orElseThrow();

        point.cancel(reservation.getReservedAmount());
        reservation.cancel();

        pointRepository.save(point);
        pointReservationsRepository.save(reservation);
    }
}
