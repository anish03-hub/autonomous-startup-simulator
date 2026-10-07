package com.startupsimulator.service;

import com.startupsimulator.dto.response.BlueprintDto;
import com.startupsimulator.dto.response.MvpFeatureDetailDto;
import com.startupsimulator.model.ExecutionTask;
import com.startupsimulator.model.enums.AgentType;
import com.startupsimulator.model.enums.TaskPriority;
import com.startupsimulator.repository.ExecutionTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates an executable task graph (DAG) from any arbitrary persisted Startup Blueprint.
 * Ensures tasks are dynamically tailored to the blueprint's MVP features, technical architecture,
 * marketing strategy, and financial budget allocations.
 */
@Service
@RequiredArgsConstructor
public class TaskGenerator {

    private final ExecutionTaskRepository taskRepository;

    @Transactional
    public List<ExecutionTask> generateTasks(Long startupId, BlueprintDto blueprint) {
        // Clear any previous execution tasks for clean generation
        taskRepository.deleteByStartupId(startupId);

        List<ExecutionTask> generated = new ArrayList<>();

        // 1. CEO TASKS
        ExecutionTask ceoTask1 = new ExecutionTask(
                startupId,
                AgentType.CEO,
                "Alex Vance (CEO)",
                "Strategic Alignment & Execution Kickoff",
                "Define strategic roadmap milestones based on vision: " + (blueprint != null && blueprint.pitch() != null ? blueprint.pitch() : "Startup Vision"),
                TaskPriority.HIGH,
                3
        );
        ceoTask1 = taskRepository.save(ceoTask1);
        generated.add(ceoTask1);

        // 2. DEVELOPMENT TASKS
        ExecutionTask devTask1 = new ExecutionTask(
                startupId,
                AgentType.DEVELOPMENT,
                "Dev Lead",
                "Repo & Environment Architecture Setup",
                "Initialize version control, CI/CD pipeline, and core tech stack: " +
                        (blueprint != null && blueprint.technical() != null && blueprint.technical().techStack() != null
                                ? blueprint.technical().techStack()
                                : "Standard Stack"),
                TaskPriority.HIGH,
                4
        );
        devTask1.setDependencies(List.of(ceoTask1.getId()));
        devTask1 = taskRepository.save(devTask1);
        generated.add(devTask1);

        ExecutionTask devTask2 = new ExecutionTask(
                startupId,
                AgentType.DEVELOPMENT,
                "Backend Eng",
                "Core Database & API Infrastructure",
                "Implement foundational backend models, API routes, and integration layer: " +
                        (blueprint != null && blueprint.technical() != null && blueprint.technical().architecture() != null
                                ? blueprint.technical().architecture()
                                : "Microservices / Monolith Architecture"),
                TaskPriority.HIGH,
                6
        );
        devTask2.setDependencies(List.of(devTask1.getId()));
        devTask2 = taskRepository.save(devTask2);
        generated.add(devTask2);

        // Dynamic MVP Feature Implementation Tasks
        List<MvpFeatureDetailDto> mvpFeatures = blueprint != null && blueprint.mvpSection() != null
                ? blueprint.mvpSection().mvpFeatures()
                : null;

        Long lastDevId = devTask2.getId();
        if (mvpFeatures != null && !mvpFeatures.isEmpty()) {
            for (int i = 0; i < Math.min(mvpFeatures.size(), 3); i++) {
                MvpFeatureDetailDto feature = mvpFeatures.get(i);
                ExecutionTask featureTask = new ExecutionTask(
                        startupId,
                        AgentType.DEVELOPMENT,
                        "Dev Lead",
                        "Build MVP: " + feature.name(),
                        feature.description() != null ? feature.description() : "Implement core MVP feature functionality.",
                        TaskPriority.HIGH,
                        Math.max(4, Math.min(feature.effort() * 2, 8))
                );
                featureTask.setDependencies(List.of(lastDevId));
                featureTask = taskRepository.save(featureTask);
                generated.add(featureTask);
                lastDevId = featureTask.getId();
            }
        } else {
            ExecutionTask defaultMvpTask = new ExecutionTask(
                    startupId,
                    AgentType.DEVELOPMENT,
                    "Dev Lead",
                    "Build Core MVP Prototype",
                    "Develop primary user workflow and core value proposition feature set.",
                    TaskPriority.HIGH,
                    7
            );
            defaultMvpTask.setDependencies(List.of(lastDevId));
            defaultMvpTask = taskRepository.save(defaultMvpTask);
            generated.add(defaultMvpTask);
            lastDevId = defaultMvpTask.getId();
        }

        ExecutionTask devTaskQa = new ExecutionTask(
                startupId,
                AgentType.DEVELOPMENT,
                "Backend Eng",
                "QA, Performance & Security Audit",
                "Execute integration testing, security hardening, and load testing prior to launch.",
                TaskPriority.MEDIUM,
                4
        );
        devTaskQa.setDependencies(List.of(lastDevId));
        devTaskQa = taskRepository.save(devTaskQa);
        generated.add(devTaskQa);

        // 3. MARKETING TASKS
        ExecutionTask mktTask1 = new ExecutionTask(
                startupId,
                AgentType.MARKETING,
                "Marketing Lead",
                "Brand Identity & Messaging Framework",
                "Establish core brand guidelines, value proposition messaging, and persona positioning: " +
                        (blueprint != null && blueprint.gtmSection() != null && blueprint.gtmSection().positioning() != null
                                ? blueprint.gtmSection().positioning()
                                : "Brand Positioning"),
                TaskPriority.MEDIUM,
                4
        );
        mktTask1.setDependencies(List.of(ceoTask1.getId()));
        mktTask1 = taskRepository.save(mktTask1);
        generated.add(mktTask1);

        ExecutionTask mktTask2 = new ExecutionTask(
                startupId,
                AgentType.MARKETING,
                "Growth Lead",
                "Landing Page & Conversion Funnel",
                "Design and deploy responsive product landing page with email capture and CTA analytics.",
                TaskPriority.HIGH,
                5
        );
        mktTask2.setDependencies(List.of(mktTask1.getId()));
        mktTask2 = taskRepository.save(mktTask2);
        generated.add(mktTask2);

        ExecutionTask mktTask3 = new ExecutionTask(
                startupId,
                AgentType.MARKETING,
                "Growth Lead",
                "Growth Channels & Launch Campaign",
                "Set up customer acquisition channels, social proof, and launch strategy: " +
                        (blueprint != null && blueprint.marketing() != null && blueprint.marketing().channels() != null
                                ? blueprint.marketing().channels()
                                : "Digital Channels"),
                TaskPriority.HIGH,
                7
        );
        mktTask3.setDependencies(List.of(mktTask2.getId(), devTaskQa.getId()));
        mktTask3 = taskRepository.save(mktTask3);
        generated.add(mktTask3);

        // 4. FINANCE TASKS
        ExecutionTask finTask1 = new ExecutionTask(
                startupId,
                AgentType.FINANCE,
                "Finance Dir",
                "Capital Allocation & Banking Setup",
                "Set up financial accounts, budget allocation controls, and initial treasury management.",
                TaskPriority.HIGH,
                3
        );
        finTask1.setDependencies(List.of(ceoTask1.getId()));
        finTask1 = taskRepository.save(finTask1);
        generated.add(finTask1);

        ExecutionTask finTask2 = new ExecutionTask(
                startupId,
                AgentType.FINANCE,
                "Controller",
                "Pricing Model & Unit Economics Audit",
                "Finalize pricing tiers, calculate customer acquisition cost (CAC) & lifetime value (LTV) assumptions: " +
                        (blueprint != null && blueprint.marketing() != null && blueprint.marketing().pricingStrategy() != null
                                ? blueprint.marketing().pricingStrategy()
                                : "Tiered Subscription"),
                TaskPriority.MEDIUM,
                5
        );
        finTask2.setDependencies(List.of(finTask1.getId(), mktTask1.getId()));
        finTask2 = taskRepository.save(finTask2);
        generated.add(finTask2);

        ExecutionTask finTask3 = new ExecutionTask(
                startupId,
                AgentType.FINANCE,
                "Controller",
                "Burn Rate & Cashflow Financial Controls",
                "Establish monthly operating burn monitoring, runway alerts, and financial report dashboard.",
                TaskPriority.HIGH,
                6
        );
        finTask3.setDependencies(List.of(finTask2.getId(), mktTask3.getId()));
        finTask3 = taskRepository.save(finTask3);
        generated.add(finTask3);

        // Final CEO Review Task
        ExecutionTask ceoTaskFinal = new ExecutionTask(
                startupId,
                AgentType.CEO,
                "Alex Vance (CEO)",
                "Product-Market Fit & Milestone Review",
                "Review MVP launch results, unit economics, and approve next stage growth expansion.",
                TaskPriority.HIGH,
                4
        );
        ceoTaskFinal.setDependencies(List.of(mktTask3.getId(), finTask3.getId()));
        ceoTaskFinal = taskRepository.save(ceoTaskFinal);
        generated.add(ceoTaskFinal);

        return generated;
    }
}
