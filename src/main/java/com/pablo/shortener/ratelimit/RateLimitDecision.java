package com.pablo.shortener.ratelimit;

/** O resultado de uma checagem de rate limit. */
public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {

    static RateLimitDecision allow() {
        return new RateLimitDecision(true, 0);
    }

    static RateLimitDecision reject(long retryAfterSeconds) {
        return new RateLimitDecision(false, retryAfterSeconds);
    }
}
