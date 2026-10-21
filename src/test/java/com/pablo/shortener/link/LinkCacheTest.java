package com.pablo.shortener.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pablo.shortener.config.AppProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

/** Testa o LinkCache sem Redis de verdade: o Mockito simula o Redis falhando. */
class LinkCacheTest {

    private ValueOperations<String, String> valueOps;
    private LinkCache linkCache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        AppProperties props = new AppProperties("http://localhost:8080", new AppProperties.Cache(Duration.ofHours(24)));
        linkCache = new LinkCache(redis, JsonMapper.builder().build(), props);
    }

    @Test
    void redisForaDoArNaLeituraViraCacheMiss() {
        when(valueOps.get(anyString())).thenThrow(new RedisConnectionFailureException("Redis fora do ar"));

        assertThat(linkCache.get("6LAzf")).isEmpty();
    }

    @Test
    void redisForaDoArNaGravacaoNaoQuebraARequisicao() {
        org.mockito.Mockito.doThrow(new RedisConnectionFailureException("Redis fora do ar"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        assertThatCode(() -> linkCache.put("6LAzf", new CachedLink(1, "https://example.com")))
                .doesNotThrowAnyException();
    }

    @Test
    void valorCorrompidoNoCacheViraCacheMiss() {
        when(valueOps.get(anyString())).thenReturn("isto não é json");

        assertThat(linkCache.get("6LAzf")).isEmpty();
    }

    @Test
    void leOValorGuardadoComoJson() {
        when(valueOps.get("link:code:6LAzf")).thenReturn("{\"id\":1,\"originalUrl\":\"https://example.com\"}");

        assertThat(linkCache.get("6LAzf")).contains(new CachedLink(1, "https://example.com"));
    }
}
