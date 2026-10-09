package com.example.product.application;

import com.example.product.application.dto.ProductBuyCancelCommand;
import com.example.product.application.dto.ProductBuyCancelResult;
import com.example.product.application.dto.ProductBuyCommand;
import com.example.product.application.dto.ProductBuyResult;
import com.example.product.domain.Product;
import com.example.product.domain.ProductTransactionHistory;
import com.example.product.infrastructure.ProductRepository;
import com.example.product.infrastructure.ProductTransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductTransactionHistoryRepository productTransactionHistoryRepository;

    @Transactional
    public ProductBuyResult buy(ProductBuyCommand command) {
        List<ProductTransactionHistory> histories = productTransactionHistoryRepository.findAllByRequestIdAndTransactionType(
                command.requestId(),
                ProductTransactionHistory.TransactionType.PURCHASE
        );

        if (!histories.isEmpty()) {
            System.out.println("이미 구매한 이력이 있습니다.");

            long totalPrice = histories.stream()
                    .mapToLong(ProductTransactionHistory::getPrice)
                    .sum();

            return new ProductBuyResult(totalPrice);
        }

        Long totalPrice = 0L;

        for (ProductBuyCommand.ProductInfo productInfo : command.productInfos()) {
            Product product = productRepository.findById(productInfo.productId()).orElseThrow();

            product.buy(productInfo.quantity());
            Long price = product.calculatePrice(productInfo.quantity());
            totalPrice += price;

            productTransactionHistoryRepository.save(
                    new ProductTransactionHistory(
                            command.requestId(),
                            productInfo.productId(),
                            productInfo.quantity(),
                            price,
                            ProductTransactionHistory.TransactionType.PURCHASE
                    )
            );
        }

        return new ProductBuyResult(totalPrice);
    }

    @Transactional
    public ProductBuyCancelResult cancel(ProductBuyCancelCommand command) {
        List<ProductTransactionHistory> buyHistories = productTransactionHistoryRepository.findAllByRequestIdAndTransactionType(
                command.requestId(),
                ProductTransactionHistory.TransactionType.PURCHASE
        );

        if (buyHistories.isEmpty()) {
            throw new RuntimeException("구매이력이 존재하지 않습니다.");
        }

        List<ProductTransactionHistory> cancelHistories = productTransactionHistoryRepository.findAllByRequestIdAndTransactionType(
                command.requestId(),
                ProductTransactionHistory.TransactionType.CANCEL
        );

        if (!cancelHistories.isEmpty()) {
            System.out.println("이미 취소되었습니다.");
            long totalPrice = cancelHistories.stream()
                    .mapToLong(ProductTransactionHistory::getPrice)
                    .sum();

            return new ProductBuyCancelResult(totalPrice);
        }

        Long totalPrice = 0L;

        for (ProductTransactionHistory buyHistory : buyHistories) {
            Product product = productRepository.findById(buyHistory.getProductId()).orElseThrow();
            product.cancel(buyHistory.getQuantity());
            totalPrice += product.calculatePrice(buyHistory.getQuantity());

            productTransactionHistoryRepository.save(
                    new  ProductTransactionHistory(
                            command.requestId(),
                            buyHistory.getProductId(),
                            buyHistory.getQuantity(),
                            buyHistory.getPrice(),
                            ProductTransactionHistory.TransactionType.CANCEL
                    )
            );
        }

        return new  ProductBuyCancelResult(totalPrice);
    }
}
