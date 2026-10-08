package com.startupsimulator.resilience;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.startupsimulator.agent.AgentAnalysisException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * Distinguishes retryable transient provider/network failures from non-retryable
 * validation, configuration, or programmer errors.
 */
public class FailureClassifier {

    public enum FailureType {
        RETRYABLE_TRANSIENT,
        NON_RETRYABLE_VALIDATION,
        NON_RETRYABLE_CONFIG,
        NON_RETRYABLE_FATAL
    }

    public static boolean isRetryable(Throwable t) {
        return classify(t) == FailureType.RETRYABLE_TRANSIENT;
    }

    public static FailureType classify(Throwable t) {
        if (t == null) {
            return FailureType.NON_RETRYABLE_FATAL;
        }

        // Fatal programmer errors surface immediately
        if (t instanceof NullPointerException ||
            t instanceof IllegalArgumentException ||
            t instanceof UnsupportedOperationException) {
            return FailureType.NON_RETRYABLE_FATAL;
        }

        // Json parsing or application structured validation errors are non-retryable
        if (t instanceof JsonProcessingException || t instanceof AgentAnalysisException) {
            return FailureType.NON_RETRYABLE_VALIDATION;
        }

        String msg = t.getMessage() != null ? t.getMessage() : "";

        // Unconfigured key / auth / validation issues
        if (msg.contains("API key is not configured") ||
            msg.contains("failed validation") ||
            msg.contains("Failed to parse structured LLM response") ||
            msg.contains("Malformed OpenAI response envelope") ||
            msg.contains("Malformed Gemini response envelope") ||
            msg.contains("Malformed JustDoWork response envelope")) {
            return FailureType.NON_RETRYABLE_VALIDATION;
        }

        // 4xx client errors (except 429 Too Many Requests) are non-retryable (400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found)
        if (t instanceof HttpClientErrorException && !(t instanceof HttpClientErrorException.TooManyRequests)) {
            return FailureType.NON_RETRYABLE_VALIDATION;
        }

        // Direct transient network / timeout types
        if (t instanceof ResourceAccessException ||
            t instanceof SocketTimeoutException ||
            t instanceof ConnectException ||
            t instanceof TimeoutException ||
            t instanceof HttpServerErrorException ||
            t instanceof HttpClientErrorException.TooManyRequests) {
            return FailureType.RETRYABLE_TRANSIENT;
        }

        // Generic RestClientException or general network IOException (excluding JsonProcessingException)
        if (t instanceof RestClientException) {
            return FailureType.RETRYABLE_TRANSIENT;
        }

        if (t instanceof IOException && !(t instanceof JsonProcessingException)) {
            return FailureType.RETRYABLE_TRANSIENT;
        }

        // Unwrap cause if wrapped (e.g. in CeoAnalysisException)
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            FailureType causeType = classify(cause);
            if (causeType != FailureType.NON_RETRYABLE_FATAL) {
                return causeType;
            }
        }

        // Fallback for wrapped exception message clues
        if (msg.contains("ResourceAccessException") ||
            msg.contains("HttpServerErrorException") ||
            msg.contains("TooManyRequests") ||
            msg.contains("SocketTimeoutException") ||
            msg.contains("ConnectException") ||
            msg.contains("transient") ||
            msg.contains("timeout")) {
            return FailureType.RETRYABLE_TRANSIENT;
        }

        return FailureType.NON_RETRYABLE_FATAL;
    }
}
