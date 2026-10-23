package com.pablo.shortener.ratelimit;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Limite de criação de links atingido. Tente novamente em " + retryAfterSeconds + " segundo(s).");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
