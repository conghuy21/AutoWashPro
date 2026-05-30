package com.autowashpro.repository;

import com.autowashpro.entity.WalkInCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WalkInRepository extends JpaRepository<WalkInCustomer, Integer> {
    List<WalkInCustomer> findAllByOrderByCreatedAtDesc();
    List<WalkInCustomer> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end);
    List<WalkInCustomer> findByStatus(String status);
}