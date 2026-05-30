package com.autowashpro.repository;

import com.autowashpro.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    Optional<Employee> findByUser_Email(String email);
    Optional<Employee> findByUser_UserId(Integer userId);

}