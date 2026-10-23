package com.pablo.shortener.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Quando a aplicação termina de subir, manda um PING para o Redis.
 *
 * <p>Serve para duas coisas: (1) já abrir a conexão (com o aperto de mão TLS), para a primeira
 * requisição de um usuário não pagar esse custo e estourar o timeout; (2) deixar no log, logo no
 * início, se a conexão funcionou ou qual foi o erro (senha, TLS, host errado...).
 *
 * <p>Tenta algumas vezes porque a primeira conexão é a mais lenta (aperto de mão TLS, JVM "fria").
 * Depois de aberta, a conexão é reaproveitada por todas as requisições.
 * Uma falha aqui não derruba a aplicação: o Redis é opcional.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStartupCheck {

    private static final int ATTEMPTS = 3;
    private static final long PAUSE_MILLIS = 1_000;

    private final StringRedisTemplate redis;

    @EventListener(ApplicationReadyEvent.class)
    public void ping() {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            long start = System.nanoTime();
            try {
                String reply = redis.execute((RedisCallback<String>) RedisConnection::ping);
                log.info("Conexão com o Redis OK ({} em {} ms, tentativa {})",
                        reply, (System.nanoTime() - start) / 1_000_000, attempt);
                return;
            } catch (RuntimeException e) {
                if (attempt == ATTEMPTS) {
                    log.warn("Não foi possível conectar ao Redis na inicialização (a aplicação segue sem cache "
                            + "e sem rate limit até ele voltar): {}", RedisErrors.describe(e));
                    return;
                }
                log.info("Tentativa {} de conectar ao Redis falhou ({}); tentando de novo",
                        attempt, RedisErrors.describe(e));
                sleep();
            }
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(PAUSE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
