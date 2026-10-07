package com.startupsimulator.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single milestone on the startup roadmap (rows form the roadmap). */
@Entity
@Table(name = "roadmap_milestones")
@Getter
@Setter
@NoArgsConstructor
public class RoadmapMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Column(nullable = false)
    private String phase;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** e.g. "Month 1-2". */
    private String timeframe;

    private int displayOrder = 0;

    public RoadmapMilestone(Long startupId, String phase, String title, String description,
                            String timeframe, int displayOrder) {
        this.startupId = startupId;
        this.phase = phase;
        this.title = title;
        this.description = description;
        this.timeframe = timeframe;
        this.displayOrder = displayOrder;
    }
}
