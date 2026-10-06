package com.example.product.application;

import com.example.product.application.dto.ProductReserveCancelCommand;
import com.example.product.application.dto.ProductReserveCommand;
import com.example.product.application.dto.ProductReserveConfirmCommand;
import com.example.product.application.dto.ProductReserveResult;
import com.example.product.domain.Product;
import com.example.product.domain.ProductReservation;
import com.example.product.infrastructure.ProductRepository;
import com.example.product.infrastructure.ProductReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductFacadeService productFacadeService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductReservationRepository productReservationRepository;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        productReservationRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();

        product1 = productRepository.save(new Product(100L, 100L));
    }

    @Test
    void 상품을_예약하면_예약_수량이_증가하고_예약_내역이_저장된다() {
        // given
        ProductReserveCommand command = new ProductReserveCommand("request-1", List.of(
                new ProductReserveCommand.ReserveItem(product1.getId(), 2L)
        ));

        // when
        ProductReserveResult result = productFacadeService.tryReserve(command);

        // then
        assertThat(result.totalPrice()).isEqualTo(200L); // 2개 * 100원 = 200원
        assertThat(productRepository.findById(product1.getId()).orElseThrow().getReservedQuantity()).isEqualTo(2L); // 예약수량 2

        List<ProductReservation> reservations = productReservationRepository.findAllByRequestId("request-1");
        assertThat(reservations).hasSize(1); // 예약 사이즈 1
        assertThat(reservations)
                .extracting(ProductReservation::getProductId, ProductReservation::getReservedQuantity, ProductReservation::getReservedPrice)
                .containsExactlyInAnyOrder(
                        tuple(product1.getId(), 2L, 200L)
                );
        assertThat(reservations)
                .allMatch(reservation -> reservation.getStatus() == ProductReservation.ProductReservationStatus.RESERVED);
    }

    @Test
    void 예약_가능한_수량을_초과하면_재시도하지_않고_원래_예외를_던진다() {
        // given
        // 상품1 재고 100개
        ProductReserveCommand command = new ProductReserveCommand("request-1", List.of(
                new ProductReserveCommand.ReserveItem(product1.getId(), 101L)
        ));

        // when & then
        assertThatThrownBy(() -> productFacadeService.tryReserve(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("예약할 수 있는 수량이 부족합니다.");

        assertThat(productRepository.findById(product1.getId()).orElseThrow().getReservedQuantity()).isZero();
        assertThat(productReservationRepository.findAllByRequestId("request-1")).isEmpty();
    }

    @Test
    void 예약을_확정하면_재고와_예약_수량이_차감되고_예약_상태가_CONFIRMED가_된다() {
        // given
        // 상품1 재고 100개 중 1개 예약
        ProductReserveCommand reserveCommand = new ProductReserveCommand("request-1", List.of(
                new ProductReserveCommand.ReserveItem(product1.getId(), 1L)
        ));
        productFacadeService.tryReserve(reserveCommand);

        // when
        ProductReserveConfirmCommand confirmCommand = new ProductReserveConfirmCommand("request-1");
        productFacadeService.confirmReserve(confirmCommand);

        // then
        Product product = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(product.getQuantity()).isEqualTo(99L); // 재고 100 - 1
        assertThat(product.getReservedQuantity()).isZero(); // 확정된 만큼 예약 수량 해제

        assertThat(productReservationRepository.findAllByRequestId("request-1"))
                .hasSize(1)
                .allMatch(reservation -> reservation.getStatus() == ProductReservation.ProductReservationStatus.CONFIRMED);
    }

    @Test
    void 확정_전에_예약을_취소하면_예약_수량이_원상복구되고_예약_상태가_CANCELED가_된다() {
        // given
        // 상품1 재고 100개 중 2개 예약
        ProductReserveCommand reserveCommand = new ProductReserveCommand("request-1", List.of(
                new ProductReserveCommand.ReserveItem(product1.getId(), 2L)
        ));
        productFacadeService.tryReserve(reserveCommand);

        // 취소 전: 재고는 그대로, 예약 수량만 2 증가
        Product reserved = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(reserved.getQuantity()).isEqualTo(100L);
        assertThat(reserved.getReservedQuantity()).isEqualTo(2L);

        // when
        ProductReserveCancelCommand cancelCommand = new ProductReserveCancelCommand("request-1");
        productFacadeService.cancelResolved(cancelCommand);

        // then
        Product product = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(product.getQuantity()).isEqualTo(100L); // 재고는 차감되지 않음
        assertThat(product.getReservedQuantity()).isZero(); // 예약 수량 원상복구

        assertThat(productReservationRepository.findAllByRequestId("request-1"))
                .hasSize(1)
                .allMatch(reservation -> reservation.getStatus() == ProductReservation.ProductReservationStatus.CANCELED);
    }
}
