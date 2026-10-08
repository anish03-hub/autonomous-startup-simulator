package com.startupsimulator.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Reusable execution wrapper providing bounded exponential backoff retries for transient LLM calls.
 */
@Component
public class RetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    private final Sleeper sleeper;

    public RetryExecutor() {
        this(Sleeper.SYSTEM);
    }

    public RetryExecutor(Sleeper sleeper) {
        this.sleeper = sleeper != null ? sleeper : Sleeper.SYSTEM;
    }

    public <T> T execute(String operationName, Supplier<T> operation, RetryPolicy policy) {
        return execute(operationName, operation, policy, null);
    }

    public <T> T execute(String operationName, Supplier<T> operation, RetryPolicy policy, RetryListener listener) {
        RetryPolicy activePolicy = policy != null ? policy : new RetryPolicy();
        int attempt = 0;
        Throwable lastFailure = null;

        while (attempt < activePolicy.getMaxAttempts()) {
            attempt++;
            try {
                T result = operation.get();
                if (attempt > 1) {
                    log.info("Operation '{}' succeeded after retry (attempt {} of {})",
                            operationName, attempt, activePolicy.getMaxAttempts());
                }
                return result;
            } catch (Throwable t) {
                lastFailure = t;
                boolean retryable = FailureClassifier.isRetryable(t);
                FailureClassifier.FailureType classification = FailureClassifier.classify(t);

                if (!retryable || attempt >= activePolicy.getMaxAttempts()) {
                    if (!retryable) {
                        log.warn("Operation '{}' failed with non-retryable failure (attempt {} of {}): [{}] {}",
                                operationName, attempt, activePolicy.getMaxAttempts(), classification, t.getMessage());
                        if (listener != null) {
                            listener.onNonRetryableFailure(operationName, attempt, activePolicy.getMaxAttempts(), classification, t);
                        }
                        throw t;
                    }

                    // Retries exhausted
                    log.error("Operation '{}' retry exhausted after {} attempt(s). Last failure: [{}] {}",
                            operationName, attempt, classification, t.getMessage());
                    if (listener != null) {
                        listener.onRetryExhausted(operationName, attempt, activePolicy.getMaxAttempts(), classification, t);
                    }
                    throw new RetryExhaustedException(operationName, attempt, t);
                }

                long backoffMs = activePolicy.calculateBackoffMs(attempt);
                log.warn("Operation '{}' failed transiently (attempt {} of {}): [{}] {}. Retrying in {}ms...",
                        operationName, attempt, activePolicy.getMaxAttempts(), classification, t.getMessage(), backoffMs);

                if (listener != null) {
                    listener.onRetryAttempt(operationName, attempt, activePolicy.getMaxAttempts(), classification, backoffMs, t);
                }

                try {
                    sleeper.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RetryExhaustedException(operationName, attempt, ie);
                }
            }
        }

        throw new RetryExhaustedException(operationName, attempt, lastFailure);
    }

    public interface RetryListener {
        default void onRetryAttempt(String op, int attempt, int maxAttempts, FailureClassifier.FailureType failureType, long backoffMs, Throwable cause) {}
        default void onRetryExhausted(String op, int attempts, int maxAttempts, FailureClassifier.FailureType failureType, Throwable cause) {}
        default void onNonRetryableFailure(String op, int attempt, int maxAttempts, FailureClassifier.FailureType failureType, Throwable cause) {}
    }
}
