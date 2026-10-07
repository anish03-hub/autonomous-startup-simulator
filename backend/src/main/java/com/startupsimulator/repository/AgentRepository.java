package com.startupsimulator.repository;

import com.startupsimulator.model.Agent;
import com.startupsimulator.model.enums.AgentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findByStartupIdOrderById(Long startupId);
    Optional<Agent> findByStartupIdAndType(Long startupId, AgentType type);
}
