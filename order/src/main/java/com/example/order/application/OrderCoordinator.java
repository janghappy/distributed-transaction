package com.example.order.application;

import com.example.order.application.dto.OrderDto;
import com.example.order.application.dto.PlaceOrderCommand;
import com.example.order.infrastructure.point.PointApiClient;
import com.example.order.infrastructure.point.dto.PointReserveApiRequest;
import com.example.order.infrastructure.point.dto.PointReserveCancelApiRequest;
import com.example.order.infrastructure.point.dto.PointReserveConfirmApiRequest;
import com.example.order.infrastructure.product.ProductApiClient;
import com.example.order.infrastructure.product.dto.ProductReserveApiRequest;
import com.example.order.infrastructure.product.dto.ProductReserveApiResponse;
import com.example.order.infrastructure.product.dto.ProductReserveCancelApiRequest;
import com.example.order.infrastructure.product.dto.ProductReserveConfirmApiRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OrderCoordinator {

    private final OrderService orderService;
    private final ProductApiClient productApiClient;
    private final PointApiClient pointApiClient;

    public void placeOrder(PlaceOrderCommand command){
        reserve(command.orderId());
        confirm(command.orderId());
    }

    private void reserve(Long orderId) {
        OrderDto orderInfo = orderService.getOrder(orderId);
        String requestId = orderInfo.requestId().toString();
        orderService.reserve(orderId);

        try{
            ProductReserveApiRequest productReserveApiRequest = new ProductReserveApiRequest(
                    requestId,
                    orderInfo.orderItems().stream()
                            .map(
                                    orderItem -> new ProductReserveApiRequest.ReserveItem(
                                            orderItem.productId(),
                                            orderItem.quantity()
                                    )
                            ).toList()
            );

            ProductReserveApiResponse productReserveApiResponse = productApiClient.reserve(productReserveApiRequest);

            PointReserveApiRequest pointReserveApiRequest = new PointReserveApiRequest(
                    requestId,
                    1L,
                    productReserveApiResponse.totalPrice()
            );

            pointApiClient.reserve(pointReserveApiRequest);
        }catch (Exception e) {
            orderService.cancel(orderId);

            // 예약이 어디까지 진행됐는지 알 수 없으므로 상품·포인트 모두 취소를 시도한다.
            // 예약되지 않은 쪽은 취소 시 예외가 나므로, 한쪽 취소 실패가 다른 쪽 취소를 막지 않도록 각각 처리한다.
            try {
                productApiClient.cancel(new ProductReserveCancelApiRequest(requestId));
            } catch (Exception ignored) {
            }

            try {
                pointApiClient.cancel(new PointReserveCancelApiRequest(requestId));
            } catch (Exception ignored) {
            }

            // 예약 실패 시 확정 단계로 넘어가지 않도록 예외를 다시 던진다.
            throw e;
        }
    }

    public void confirm(Long orderId){
        String requestId = orderService.getOrder(orderId).requestId().toString();
        try{
            ProductReserveConfirmApiRequest productReserveConfirmApiRequest = new ProductReserveConfirmApiRequest(requestId);
            productApiClient.confirm(productReserveConfirmApiRequest);

            PointReserveConfirmApiRequest pointReserveConfirmApiRequest = new PointReserveConfirmApiRequest(requestId);
            pointApiClient.confirm(pointReserveConfirmApiRequest);

            orderService.confirm(orderId);
        }catch (Exception e) {
            orderService.pending(orderId);
        }
    }

}
