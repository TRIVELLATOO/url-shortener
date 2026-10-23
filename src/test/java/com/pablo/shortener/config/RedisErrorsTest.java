package com.pablo.shortener.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;

class RedisErrorsTest {

    @Test
    void mostraACausaRaizEmbrulhada() {
        var e = new RedisConnectionFailureException("Unable to connect to Redis",
                new IllegalStateException("wrapper", new TimeoutException("SSL handshake timed out")));

        assertThat(RedisErrors.describe(e))
                .contains("RedisConnectionFailureException: Unable to connect to Redis")
                .contains("causa: TimeoutException: SSL handshake timed out");
    }

    @Test
    void semCausaMostraSoOErro() {
        var e = new RedisConnectionFailureException("Redis command timed out");

        assertThat(RedisErrors.describe(e)).isEqualTo("RedisConnectionFailureException: Redis command timed out");
    }
}
