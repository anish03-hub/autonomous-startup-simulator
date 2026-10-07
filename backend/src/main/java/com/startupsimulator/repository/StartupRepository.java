package com.startupsimulator.repository;

import com.startupsimulator.model.Startup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StartupRepository extends JpaRepository<Startup, Long> {
    List<Startup> findAllByOrderByCreatedAtDesc();
}
