package com.startupsimulator.resilience;

import com.startupsimulator.agent.LLMService;

/**
 * Decorator for wrapping any {@link LLMService} implementation with bounded retry + backoff behavior.
 */
public class RetryableLLMService implements LLMService {

    private final LLMService delegate;
    private final RetryExecutor retryExecutor;
    private final RetryPolicy retryPolicy;

    public RetryableLLMService(LLMService delegate, RetryExecutor retryExecutor, RetryPolicy retryPolicy) {
        this.delegate = delegate;
        this.retryExecutor = retryExecutor != null ? retryExecutor : new RetryExecutor();
        this.retryPolicy = retryPolicy != null ? retryPolicy : new RetryPolicy();
    }

    @Override
    public String complete(String system, String user) {
        return delegate.complete(system, user);
    }

    @Override
    public String generate(String system, String user) {
        if (!isRealProvider()) {
            return delegate.generate(system, user);
        }
        return retryExecutor.execute("LLMService.generate", () -> delegate.generate(system, user), retryPolicy);
    }

    @Override
    public <T> T generateStructured(String system, String user, Class<T> type) {
        if (!isRealProvider()) {
            return delegate.generateStructured(system, user, type);
        }
        return retryExecutor.execute("LLMService.generateStructured",
                () -> delegate.generateStructured(system, user, type), retryPolicy);
    }

    @Override
    public String provider() {
        return delegate.provider();
    }

    @Override
    public boolean isRealProvider() {
        return delegate.isRealProvider();
    }

    public LLMService getDelegate() {
        return delegate;
    }
}
