package com.pablo.shortener.link;

/**
 * Converte o id numérico de um link em um código curto (e vice-versa) usando Base62.
 *
 * <p>Um deslocamento fixo é somado ao id antes de codificar, para que os códigos já
 * comecem com 5 caracteres (o id 1 vira "6LAzf") em vez de "1", "2", "3".
 * Observação: o deslocamento não torna os códigos imprevisíveis; eles continuam sequenciais.
 */
public final class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    /** 62^4 < 100.000.000 < 62^5, então o primeiro código já tem 5 caracteres. */
    static final long OFFSET = 100_000_000L;

    private Base62Encoder() {
    }

    public static String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("O id não pode ser negativo: " + id);
        }
        long value = Math.addExact(id, OFFSET);

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        // Os dígitos saem do menos significativo para o mais significativo, então invertemos.
        return sb.reverse().toString();
    }

    public static long decode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("O código não pode ser vazio");
        }
        long value = 0;
        for (char c : code.toCharArray()) {
            int digit = ALPHABET.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Caractere inválido no código: " + c);
            }
            value = Math.addExact(Math.multiplyExact(value, BASE), digit);
        }
        long id = value - OFFSET;
        if (id < 0) {
            throw new IllegalArgumentException("Código fora do intervalo válido: " + code);
        }
        return id;
    }
}
