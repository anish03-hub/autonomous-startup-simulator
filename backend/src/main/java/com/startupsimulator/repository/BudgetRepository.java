package com.startupsimulator.repository;

import com.startupsimulator.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByStartupId(Long startupId);
}
