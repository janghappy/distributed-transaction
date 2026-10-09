package com.example.point.application;

import com.example.point.application.dto.PointCancelCommand;
import com.example.point.application.dto.PointUseCommand;
import com.example.point.domain.Point;
import com.example.point.domain.PointTransactionHistory;
import com.example.point.infrastructure.PointRepository;
import com.example.point.infrastructure.PointTransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointTransactionHistoryRepository pointTransactionHistoryRepository;

    @Transactional
    public void use(PointUseCommand command){
        PointTransactionHistory useHistory = pointTransactionHistoryRepository.findByRequestIdAndTransactionType(
                command.requestId(),
                PointTransactionHistory.TransactionType.USE
        );

        if(useHistory != null){
            System.out.println("이미 사용한 이력이 존재합니다.");
            return;
        }

        Point point = pointRepository.findByUserId(command.userId());
        if(point == null){
            throw new RuntimeException("포인트가 존재하지 않습니다.");
        }

        point.use(command.amount());
        pointTransactionHistoryRepository.save(
                new PointTransactionHistory(
                        command.requestId(),
                        point.getId(),
                        command.amount(),
                        PointTransactionHistory.TransactionType.USE
                )
        );
    }

    @Transactional
    public void cancel(PointCancelCommand command){
        PointTransactionHistory useHistory = pointTransactionHistoryRepository.findByRequestIdAndTransactionType(
                command.requestId(),
                PointTransactionHistory.TransactionType.USE
        );

        if(useHistory == null){
            throw new RuntimeException("포인트 사용 내역이 존재하지 않습니다.");
        }

        PointTransactionHistory cancelHistory = pointTransactionHistoryRepository.findByRequestIdAndTransactionType(
                command.requestId(),
                PointTransactionHistory.TransactionType.CANCEL
        );

        if(cancelHistory != null){
            System.out.println("이미 취소된 요청입니다.");
            return;
        }

        Point point = pointRepository.findById(useHistory.getPointId()).orElseThrow();
        point.cancel(useHistory.getAmount());
        pointTransactionHistoryRepository.save(
                new PointTransactionHistory(
                        command.requestId(),
                        point.getId(),
                        useHistory.getAmount(),
                        PointTransactionHistory.TransactionType.CANCEL
                )
        );
    }

}
