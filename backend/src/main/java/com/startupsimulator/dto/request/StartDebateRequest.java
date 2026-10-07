package com.startupsimulator.dto.request;

/** Optional payload to start a specific debate topic. */
public record StartDebateRequest(String topic, String question) {
}
