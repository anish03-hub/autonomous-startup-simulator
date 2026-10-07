package com.startupsimulator.repository;

import com.startupsimulator.model.TechnicalPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TechnicalPlanRepository extends JpaRepository<TechnicalPlan, Long> {
    Optional<TechnicalPlan> findByStartupId(Long startupId);
}
