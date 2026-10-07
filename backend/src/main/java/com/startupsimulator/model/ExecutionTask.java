package com.startupsimulator.model;

import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.TaskPriority;
import com.startupsimulator.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "execution_tasks")
@Getter
@Setter
@NoArgsConstructor
public class ExecutionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long startupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentType department;

    @Column(nullable = false)
    private String assignedAgent;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(nullable = false)
    private double progress = 0.0;

    @Column(name = "dependency_ids")
    private String dependencyIdsString = ""; // Comma-separated dependency IDs

    @Column(nullable = false)
    private int estimatedDays = 5;

    @Column(nullable = false)
    private int actualDays = 0;

    @Column(columnDefinition = "TEXT")
    private String blockerReason;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public ExecutionTask(Long startupId, AgentType department, String assignedAgent, String title,
                         String description, TaskPriority priority, int estimatedDays) {
        this.startupId = startupId;
        this.department = department;
        this.assignedAgent = assignedAgent;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.estimatedDays = estimatedDays;
        this.status = TaskStatus.PENDING;
        this.progress = 0.0;
        this.actualDays = 0;
    }

    public List<Long> getDependencies() {
        if (dependencyIdsString == null || dependencyIdsString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dependencyIdsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    public void setDependencies(List<Long> depIds) {
        if (depIds == null || depIds.isEmpty()) {
            this.dependencyIdsString = "";
        } else {
            this.dependencyIdsString = depIds.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
