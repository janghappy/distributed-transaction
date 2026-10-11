package com.example.order.application;

import com.example.order.application.dto.OrderDto;
import com.example.order.application.dto.PlaceOrderCommand;
import com.example.order.domain.CompensationRegistry;
import com.example.order.infrastructure.CompensationRepository;
import com.example.order.infrastructure.point.PointApiClient;
import com.example.order.infrastructure.point.PointCancelApiRequest;
import com.example.order.infrastructure.point.PointUseApiRequest;
import com.example.order.infrastructure.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCoordinator {

    private final OrderService orderService;
    private final ProductApiClient productApiClient;
    private final PointApiClient pointApiClient;
    private final CompensationRepository compensationRepository;

    public void placeOrder(PlaceOrderCommand command){
        Long orderId = command.orderId();

        orderService.request(orderId);
        OrderDto orderDto = orderService.getOrder(orderId);

        try {
            ProductBuyApiRequest productBuyApiRequest = new ProductBuyApiRequest(
                    orderId.toString(),
                    orderDto.orderItems().stream()
                            .map(item -> new ProductBuyApiRequest.ProductInfo(item.productId(), item.quantity()))
                            .toList()
            );

            ProductBuyApiResponse buyApiResponse = productApiClient.buy(productBuyApiRequest);

            PointUseApiRequest pointUseApiRequest = new PointUseApiRequest(
                    orderId.toString(),
                    1L,
                    buyApiResponse.totalPrice()
            );

            pointApiClient.use(pointUseApiRequest);

            orderService.complete(orderId);
        } catch (Exception e) {
            rollback(orderId);
            throw e;
        }
    }

    public void rollback(Long orderId){
        try{

            ProductBuyCancelApiRequest productBuyCancelApiRequest = new ProductBuyCancelApiRequest(orderId.toString());
            ProductBuyCancelApiResponse productCancelResponse = productApiClient.cancel(productBuyCancelApiRequest);

            if(productCancelResponse.totalPrice() > 0){
                PointCancelApiRequest pointCancelApiRequest = new PointCancelApiRequest(orderId.toString());
                pointApiClient.cancel(pointCancelApiRequest);
            }

            orderService.fail(orderId);
        }catch (Exception e){
            compensationRepository.save(
                    new CompensationRegistry(orderId)
            );
            throw e;
        }
    }
}
