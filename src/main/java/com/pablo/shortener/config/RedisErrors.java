package com.pablo.shortener.config;

import org.springframework.core.NestedExceptionUtils;

/**
 * Monta uma descrição útil de um erro do Redis para o log.
 *
 * <p>O Spring embrulha os erros do driver (Lettuce): a mensagem de fora costuma ser genérica,
 * e a causa de verdade (timeout, falha de TLS, senha recusada...) fica lá dentro.
 */
public final class RedisErrors {

    private RedisErrors() {
    }

    public static String describe(Throwable e) {
        Throwable root = NestedExceptionUtils.getRootCause(e);
        String outer = e.getClass().getSimpleName() + ": " + e.getMessage();
        if (root == null || root == e) {
            return outer;
        }
        return outer + " | causa: " + root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
