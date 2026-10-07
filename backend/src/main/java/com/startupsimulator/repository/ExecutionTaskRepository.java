package com.startupsimulator.repository;

import com.startupsimulator.model.ExecutionTask;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionTaskRepository extends JpaRepository<ExecutionTask, Long> {
    List<ExecutionTask> findByStartupIdOrderById(Long startupId);
    List<ExecutionTask> findByStartupIdAndDepartment(Long startupId, AgentType department);
    List<ExecutionTask> findByStartupIdAndStatus(Long startupId, TaskStatus status);
    void deleteByStartupId(Long startupId);
}
