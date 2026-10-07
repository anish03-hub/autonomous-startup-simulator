package com.startupsimulator.repository;

import com.startupsimulator.model.MvpFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MvpFeatureRepository extends JpaRepository<MvpFeature, Long> {
    List<MvpFeature> findByStartupIdOrderByDisplayOrder(Long startupId);
    long countByStartupIdAndInMvpTrue(Long startupId);
    void deleteByStartupId(Long startupId);
}
