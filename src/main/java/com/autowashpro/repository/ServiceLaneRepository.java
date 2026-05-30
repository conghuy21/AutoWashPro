package com.autowashpro.repository;

import com.autowashpro.entity.ServiceLane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceLaneRepository extends JpaRepository<ServiceLane, Integer> {
    List<ServiceLane> findByIsAvailableTrue();
}