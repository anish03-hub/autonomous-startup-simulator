package com.startupsimulator.repository;

import com.startupsimulator.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, Long> {
    List<Decision> findByStartupIdOrderById(Long startupId);
}
