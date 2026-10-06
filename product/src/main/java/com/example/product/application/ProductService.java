package com.example.product.application;

import com.example.product.application.dto.ProductReserveCancelCommand;
import com.example.product.application.dto.ProductReserveCommand;
import com.example.product.application.dto.ProductReserveConfirmCommand;
import com.example.product.application.dto.ProductReserveResult;
import com.example.product.domain.Product;
import com.example.product.domain.ProductReservation;
import com.example.product.infrastructure.ProductRepository;
import com.example.product.infrastructure.ProductReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductReservationRepository productReservationRepository;

    protected ProductReserveResult tryReserve(ProductReserveCommand command) {
        List<ProductReservation> exists = productReservationRepository.findAllByRequestId(command.requestId());

        if (!exists.isEmpty()) {
            Long totalPrice = exists.stream().mapToLong(ProductReservation::getReservedPrice).sum();

            return new ProductReserveResult(totalPrice);
        }

        Long totalPrice = 0L;
        for (ProductReserveCommand.ReserveItem item : command.items()) {
            Product product = productRepository.findById(item.productId()).orElseThrow();
            Long price = product.reserve(item.reserveQuantity());
            totalPrice += price;

            productRepository.save(product);
            productReservationRepository.save(
                    new ProductReservation(
                            command.requestId(),
                            item.productId(),
                            item.reserveQuantity(),
                            price
                    )
            );
        }

        return new ProductReserveResult(totalPrice);
    }

    protected void confirmReserve(ProductReserveConfirmCommand command) {
        List<ProductReservation> reservations = productReservationRepository.findAllByRequestId(command.requestId());

        if (reservations.isEmpty()) {
            throw new RuntimeException("예약된 정보가 없습니다.");
        }

        boolean alreadyConfirmed = reservations.stream()
                .anyMatch(item -> item.getStatus() == ProductReservation.ProductReservationStatus.CONFIRMED);

        if (alreadyConfirmed) {
            System.out.println("이미 확정이 되었습니다.");
            return;
        }

        for (ProductReservation reservation : reservations) {
            Product product = productRepository.findById(reservation.getProductId()).orElseThrow();
            product.confirm(reservation.getReservedQuantity());
            reservation.confirm();

            productRepository.save(product);
            productReservationRepository.save(reservation);
        }

    }

    protected void cancelResolved(ProductReserveCancelCommand command) {
        List<ProductReservation> reservations = productReservationRepository.findAllByRequestId(command.requestId());

        if (reservations.isEmpty()) {
            throw new RuntimeException("예약된 정보가 존재하지 않습니다.");
        }

        boolean alreadyCancelled = reservations.stream()
                .anyMatch(item -> item.getStatus() == ProductReservation.ProductReservationStatus.CANCELED);

        if(alreadyCancelled){
            System.out.println("이미 취소된 요청입니다.");
            return;
        }

        for (ProductReservation reservation : reservations) {
            Product product = productRepository.findById(reservation.getProductId()).orElseThrow();
            product.cancel(reservation.getReservedQuantity());
            reservation.cancel();

            productRepository.save(product);
            productReservationRepository.save(reservation);
        }
    }

}
