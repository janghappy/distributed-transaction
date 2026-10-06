package com.example.monolithic.infrastructure;

import com.example.monolithic.product.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
