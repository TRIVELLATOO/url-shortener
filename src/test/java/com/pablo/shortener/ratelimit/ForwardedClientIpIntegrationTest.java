package com.pablo.shortener.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Sobe um servidor HTTP de verdade (o Tomcat), porque é ele que lê o X-Forwarded-For;
 * o MockMvc não passa pelo Tomcat. A conexão do teste vem de 127.0.0.1, que o Tomcat
 * trata como proxy interno, simulando o proxy do Render.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ForwardedClientIpIntegrationTest {

    private static final String REAL_CLIENT = "203.0.113.50";
    private static final String SPOOFED = "198.51.100.99";
    private static final String TEST_URL = "https://example.com/teste-forwarded-ip";

    @LocalServerPort
    private int port;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient http = HttpClient.newHttpClient();

    // Antes também: outros testes (MockMvc usa 127.0.0.1) podem ter deixado a chave do "proxy" no Redis.
    @BeforeEach
    @AfterEach
    void limpar() {
        // Sem @Transactional aqui (o servidor roda em outra thread), então a limpeza é manual.
        redis.delete(List.of(key(REAL_CLIENT), key(SPOOFED), key("127.0.0.1")));
        jdbcTemplate.update("delete from links where original_url = ?", TEST_URL);
    }

    @Test
    void usaOIpDoClienteQueOProxyMandaNoXForwardedFor() throws Exception {
        assertThat(criarLinkCom(REAL_CLIENT)).isEqualTo(201);

        assertThat(redis.hasKey(key(REAL_CLIENT))).isTrue();
        assertThat(redis.hasKey(key("127.0.0.1"))).isFalse(); // não usou o IP do "proxy"
    }

    @Test
    void ignoraIpFalsoQueOClienteColocaNoInicioDoCabecalho() throws Exception {
        // O cliente mandou "X-Forwarded-For: 198.51.100.99" por conta própria, e o proxy
        // acrescentou o IP real no final. O Tomcat usa o mais à direita que não é proxy.
        assertThat(criarLinkCom(SPOOFED + ", " + REAL_CLIENT)).isEqualTo(201);

        assertThat(redis.hasKey(key(REAL_CLIENT))).isTrue();
        assertThat(redis.hasKey(key(SPOOFED))).isFalse();
    }

    private int criarLinkCom(String forwardedFor) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/links"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString("{\"url\": \"" + TEST_URL + "\"}"))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static String key(String ip) {
        return CreateLinkRateLimitInterceptor.KEY_PREFIX + ip;
    }
}
