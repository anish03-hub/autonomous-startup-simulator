package com.startupsimulator.repository;

import com.startupsimulator.model.AgentTask;
import com.startupsimulator.model.enums.AgentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {
    List<AgentTask> findByStartupIdOrderById(Long startupId);
    List<AgentTask> findByStartupIdAndAgentType(Long startupId, AgentType agentType);
    Optional<AgentTask> findFirstByStartupIdAndAgentTypeOrderByIdDesc(Long startupId, AgentType agentType);
}
