package com.startupsimulator.repository;

import com.startupsimulator.model.MarketingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarketingPlanRepository extends JpaRepository<MarketingPlan, Long> {
    Optional<MarketingPlan> findByStartupId(Long startupId);
}
