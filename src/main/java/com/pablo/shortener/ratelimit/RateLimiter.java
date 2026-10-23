package com.pablo.shortener.ratelimit;

import com.pablo.shortener.config.RedisErrors;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Rate limit de "janela fixa" no Redis: cada chave tem um contador que começa em 1 na
 * primeira requisição e expira ao fim da janela.
 *
 * <p>Se o Redis falhar, a requisição é liberada (fail-open): o Redis fora do ar não pode
 * derrubar o site.
 */
@Slf4j
@Component
public class RateLimiter {

    /**
     * INCR + PEXPIRE num script Lua: o Redis executa o script inteiro de uma vez (de forma atômica).
     * Com dois comandos separados, uma falha entre eles deixaria a chave sem expiração,
     * e o IP ficaria bloqueado para sempre.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final RedisScript<List> SCRIPT = (RedisScript) RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('PTTL', KEYS[1])
            if ttl < 0 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
              ttl = tonumber(ARGV[1])
            end
            return {count, ttl}
            """, List.class);

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public RateLimitDecision tryAcquire(String key, int limit, Duration window) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> result = redis.execute(SCRIPT, List.of(key), String.valueOf(window.toMillis()));
            long count = result.get(0);
            long ttlMillis = result.get(1);

            if (count <= limit) {
                return RateLimitDecision.allow();
            }
            // Arredonda para cima: com 400ms restantes, o cliente deve esperar 1s, não 0.
            long retryAfterSeconds = Math.max(1, (ttlMillis + 999) / 1000);
            return RateLimitDecision.reject(retryAfterSeconds);
        } catch (DataAccessException e) {
            log.warn("Falha no rate limit para a chave {}; liberando a requisição: {}", key, RedisErrors.describe(e));
            log.debug("Stack trace da falha no rate limit", e);
            return RateLimitDecision.allow();
        }
    }
}
