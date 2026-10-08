package com.startupsimulator.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Spring configuration properties bound from {@code resilience.llm.*}.
 */
@Component
@ConfigurationProperties(prefix = "resilience.llm")
public class RetryProperties {

    private int maxAttempts = 3;
    private long initialBackoffMs = 250;
    private double backoffMultiplier = 2.0;
    private long maxBackoffMs = 2000;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getInitialBackoffMs() {
        return initialBackoffMs;
    }

    public void setInitialBackoffMs(long initialBackoffMs) {
        this.initialBackoffMs = initialBackoffMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public long getMaxBackoffMs() {
        return maxBackoffMs;
    }

    public void setMaxBackoffMs(long maxBackoffMs) {
        this.maxBackoffMs = maxBackoffMs;
    }

    public RetryPolicy toRetryPolicy() {
        return new RetryPolicy(maxAttempts, initialBackoffMs, backoffMultiplier, maxBackoffMs);
    }
}
