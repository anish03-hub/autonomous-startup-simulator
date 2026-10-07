package com.startupsimulator.repository;

import com.startupsimulator.model.StartupEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StartupEventRepository extends JpaRepository<StartupEvent, Long> {
    List<StartupEvent> findByStartupIdOrderById(Long startupId);
    List<StartupEvent> findByStartupIdAndIdGreaterThanOrderById(Long startupId, Long afterId);
}
