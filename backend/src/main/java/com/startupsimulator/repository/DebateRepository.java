package com.startupsimulator.repository;

import com.startupsimulator.model.Debate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DebateRepository extends JpaRepository<Debate, Long> {
    List<Debate> findByStartupIdOrderById(Long startupId);
}
