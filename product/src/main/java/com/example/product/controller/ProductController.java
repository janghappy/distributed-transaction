package com.example.product.controller;

import com.example.product.application.ProductFacadeService;
import com.example.product.application.dto.ProductReserveResult;
import com.example.product.controller.dto.ProductReserveCancelRequest;
import com.example.product.controller.dto.ProductReserveConfirmRequest;
import com.example.product.controller.dto.ProductReserveRequest;
import com.example.product.controller.dto.ProductReserveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductFacadeService productFacadeService;

    @PostMapping("/reserve")
    public ProductReserveResponse reserve(@RequestBody ProductReserveRequest request) {
        ProductReserveResult result = productFacadeService.tryReserve(request.toProductReserveCommand());

        return new ProductReserveResponse(result.totalPrice());
    }

    @PostMapping("/confirm")
    public void confirm(@RequestBody ProductReserveConfirmRequest request) {
        productFacadeService.confirmReserve(request.toProductReserveConfirmCommand());
    }

    @PostMapping("/cancel")
    public void cancel(@RequestBody ProductReserveCancelRequest request) {
        productFacadeService.cancelResolved(request.toProductReserveCancelCommand());
    }
}
