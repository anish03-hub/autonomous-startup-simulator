package com.startupsimulator.resilience;

/**
 * Abstraction for backoff sleep delay to allow zero-latency testing.
 */
@FunctionalInterface
public interface Sleeper {

    void sleep(long millis) throws InterruptedException;

    Sleeper NO_OP = millis -> {};
    Sleeper SYSTEM = Thread::sleep;
}
