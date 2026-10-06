package com.example.product.init;

import com.example.product.domain.Product;
import com.example.product.infrastructure.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TestDataCreator {

    private final ProductRepository productRepository;

    // ddl-auto: create로 기동할 때마다 테이블이 새로 만들어지므로, 기본 상품 데이터를 넣어 둔다.
    @PostConstruct
    public void createTestData() {
        productRepository.save(new Product(100L, 100L)); // 1번 상품: 재고 100개, 가격 100원
        productRepository.save(new Product(100L, 200L)); // 2번 상품: 재고 100개, 가격 200원
    }
}
