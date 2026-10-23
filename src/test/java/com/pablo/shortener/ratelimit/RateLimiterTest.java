package com.pablo.shortener.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/** Testa as decisões do RateLimiter simulando as respostas do Redis com o Mockito. */
class RateLimiterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RateLimiter rateLimiter = new RateLimiter(redis);

    @Test
    void dentroDoLimiteLibera() {
        redisResponde(10L, 30_000L);

        assertThat(rateLimiter.tryAcquire("k", 10, Duration.ofMinutes(1)).allowed()).isTrue();
    }

    @Test
    void acimaDoLimiteBloqueiaEArredondaORetryAfterParaCima() {
        redisResponde(11L, 400L); // faltam 400ms para a janela acabar

        RateLimitDecision decision = rateLimiter.tryAcquire("k", 10, Duration.ofMinutes(1));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfterSeconds()).isEqualTo(1);
    }

    @Test
    void redisForaDoArLiberaARequisicao() {
        when(redis.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("Redis fora do ar"));

        assertThat(rateLimiter.tryAcquire("k", 10, Duration.ofMinutes(1)).allowed()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private void redisResponde(long count, long ttlMillis) {
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(List.of(count, ttlMillis));
    }
}
