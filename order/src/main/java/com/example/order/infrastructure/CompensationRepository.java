package com.example.order.infrastructure;

import com.example.order.domain.CompensationRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompensationRepository extends JpaRepository<CompensationRegistry, Long> {
}
