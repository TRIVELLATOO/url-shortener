package com.pablo.shortener.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Base62EncoderTest {

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 2, 61, 62, 12_345, 999_999_999, Long.MAX_VALUE - Base62Encoder.OFFSET})
    void decodeDesfazOEncode(long id) {
        assertThat(Base62Encoder.decode(Base62Encoder.encode(id))).isEqualTo(id);
    }

    @Test
    void primeirosIdsJaGeramCodigosDeCincoCaracteres() {
        assertThat(Base62Encoder.encode(1)).hasSize(5);
        assertThat(Base62Encoder.encode(2)).hasSize(5);
    }

    @Test
    void codigoConhecidoParaOPrimeiroId() {
        // 1 + 100.000.000 = 100.000.001 = 6·62⁴ + 47·62³ + 36·62² + 35·62 + 15 → "6", "L", "A", "z", "f"
        assertThat(Base62Encoder.encode(1)).isEqualTo("6LAzf");
    }

    @Test
    void idsDiferentesGeramCodigosDiferentes() {
        assertThat(Base62Encoder.encode(1)).isNotEqualTo(Base62Encoder.encode(2));
    }

    @Test
    void codigoUsaApenasCaracteresBase62() {
        assertThat(Base62Encoder.encode(987_654_321)).matches("[0-9a-zA-Z]+");
    }

    @Test
    void encodeRejeitaIdNegativo() {
        assertThatThrownBy(() -> Base62Encoder.encode(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encodeRejeitaIdQueEstouraOLong() {
        assertThatThrownBy(() -> Base62Encoder.encode(Long.MAX_VALUE))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void decodeRejeitaCaractereInvalido() {
        assertThatThrownBy(() -> Base62Encoder.decode("abc-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decodeRejeitaCodigoAbaixoDoDeslocamento() {
        // "1" decodifica para 1, que fica abaixo do deslocamento e geraria um id negativo.
        assertThatThrownBy(() -> Base62Encoder.decode("1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
