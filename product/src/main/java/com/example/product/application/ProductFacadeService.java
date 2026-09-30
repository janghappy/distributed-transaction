package com.example.product.application;

import com.example.product.application.dto.ProductReserveCommand;
import com.example.product.application.dto.ProductReserveResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductFacadeService {

    private final ProductService productService;

    public ProductReserveResult tryReserve(ProductReserveCommand command) {
        int tryCount = 0;

        while (tryCount < 3) {
            try {
                return productService.tryReserve(command);
            } catch (Exception e) {
                tryCount++;
            }
        }

        throw new RuntimeException("예약에 실패하였습니다.");
    }


}
