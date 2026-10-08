package com.startupsimulator.resilience;

/**
 * Configuration policy defining retry and exponential backoff parameters.
 *
 * <p>Requirements:
 * <ul>
 *   <li>{@code maxAttempts}: maximum number of attempts (>= 1)</li>
 *   <li>{@code initialBackoffMs}: initial delay in milliseconds (>= 0)</li>
 *   <li>{@code backoffMultiplier}: exponential multiplier (>= 1.0)</li>
 *   <li>{@code maxBackoffMs}: upper bound on delay (>= initialBackoffMs)</li>
 * </ul>
 */
public class RetryPolicy {

    private final int maxAttempts;
    private final long initialBackoffMs;
    private final double backoffMultiplier;
    private final long maxBackoffMs;

    public RetryPolicy() {
        this(3, 250L, 2.0, 2000L);
    }

    public RetryPolicy(int maxAttempts, long initialBackoffMs, double backoffMultiplier, long maxBackoffMs) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        if (initialBackoffMs < 0) {
            throw new IllegalArgumentException("initialBackoffMs must be >= 0");
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier must be >= 1.0");
        }
        if (maxBackoffMs < initialBackoffMs) {
            throw new IllegalArgumentException("maxBackoffMs must be >= initialBackoffMs");
        }
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;
        this.backoffMultiplier = backoffMultiplier;
        this.maxBackoffMs = maxBackoffMs;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getInitialBackoffMs() {
        return initialBackoffMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public long getMaxBackoffMs() {
        return maxBackoffMs;
    }

    /**
     * Calculates the bounded backoff delay in milliseconds for a completed attempt index.
     * Attempt 1 failure -> initialBackoffMs
     * Attempt 2 failure -> min(maxBackoffMs, (long)(initialBackoffMs * multiplier))
     *
     * @param attempt 1-indexed attempt number that just failed
     * @return delay in milliseconds before next attempt
     */
    public long calculateBackoffMs(int attempt) {
        if (attempt < 1) {
            return 0;
        }
        double delay = initialBackoffMs * Math.pow(backoffMultiplier, attempt - 1);
        if (delay > maxBackoffMs) {
            return maxBackoffMs;
        }
        return (long) delay;
    }
}
