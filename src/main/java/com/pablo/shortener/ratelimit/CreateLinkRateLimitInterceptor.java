package com.pablo.shortener.ratelimit;

import com.pablo.shortener.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Roda antes do controller em POST /api/links e barra quem passou do limite.
 * Fica fora do LinkController para a regra de rate limit não se misturar com a regra de negócio.
 */
@Component
@RequiredArgsConstructor
public class CreateLinkRateLimitInterceptor implements HandlerInterceptor {

    static final String KEY_PREFIX = "ratelimit:create:";

    private final RateLimiter rateLimiter;
    private final AppProperties appProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        AppProperties.RateLimit limit = appProperties.rateLimit();
        // getRemoteAddr() é o IP de quem abriu a conexão. Atrás de um proxy/load balancer ele seria
        // o IP do proxy; aí é preciso configurar server.forward-headers-strategy (ver README).
        RateLimitDecision decision = rateLimiter.tryAcquire(
                KEY_PREFIX + request.getRemoteAddr(), limit.maxCreations(), limit.window());

        if (!decision.allowed()) {
            // Tratada no ApiExceptionHandler, que devolve 429 + Retry-After.
            throw new RateLimitExceededException(decision.retryAfterSeconds());
        }
        return true;
    }
}
