package com.pablo.shortener.ratelimit;

import com.pablo.shortener.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Roda antes do controller em POST /api/links e barra quem passou do limite.
 * Fica fora do LinkController para a regra de rate limit não se misturar com a regra de negócio.
 */
@Slf4j
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
        // Com server.forward-headers-strategy=native, o Tomcat já troca o getRemoteAddr() pelo IP
        // real do cliente vindo do X-Forwarded-For (quando a conexão vem de um proxy confiável).
        String clientIp = request.getRemoteAddr();
        log.debug("Rate limit de criação para o IP {}", clientIp);
        RateLimitDecision decision = rateLimiter.tryAcquire(
                KEY_PREFIX + clientIp, limit.maxCreations(), limit.window());

        if (!decision.allowed()) {
            // Tratada no ApiExceptionHandler, que devolve 429 + Retry-After.
            throw new RateLimitExceededException(decision.retryAfterSeconds());
        }
        return true;
    }
}
