package com.example.product.infrastructure;

import com.example.product.domain.ProductTransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductTransactionHistoryRepository extends JpaRepository<ProductTransactionHistory, Long> {
    List<ProductTransactionHistory> findAllByRequestIdAndTransactionType(String requestId, ProductTransactionHistory.TransactionType transactionType);
}
