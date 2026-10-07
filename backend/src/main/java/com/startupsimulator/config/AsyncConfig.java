package com.startupsimulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Provides the scheduler used by the {@code StartupOrchestrator} to advance the
 * mock simulation on a timeline without blocking request threads.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "simulationScheduler")
    public TaskScheduler simulationScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sim-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}
