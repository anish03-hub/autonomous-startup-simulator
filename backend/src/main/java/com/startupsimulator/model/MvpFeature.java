package com.startupsimulator.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A candidate MVP feature with priority and a target release. */
@Entity
@Table(name = "mvp_features")
@Getter
@Setter
@NoArgsConstructor
public class MvpFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "startup_id", nullable = false)
    private Long startupId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Whether the feature is included in the initial MVP (V1). */
    @Column(name = "in_mvp", nullable = false)
    private boolean inMvp = true;

    /** Target release, e.g. "V1" or "V2". */
    @Column(name = "target_release", nullable = false)
    private String targetRelease = "V1";

    /** Rough engineering effort, 1 (small) .. 5 (large). */
    private int effort = 2;

    private int displayOrder = 0;

    public MvpFeature(Long startupId, String name, String description, boolean inMvp, int effort) {
        this.startupId = startupId;
        this.name = name;
        this.description = description;
        this.inMvp = inMvp;
        this.targetRelease = inMvp ? "V1" : "V2";
        this.effort = effort;
    }
}
