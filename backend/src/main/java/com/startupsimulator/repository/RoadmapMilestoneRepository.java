package com.startupsimulator.repository;

import com.startupsimulator.model.RoadmapMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadmapMilestoneRepository extends JpaRepository<RoadmapMilestone, Long> {
    List<RoadmapMilestone> findByStartupIdOrderByDisplayOrder(Long startupId);
}
