package com.pablo.shortener.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RateLimitIntegrationTest {

    private static final String IP_A = "203.0.113.10"; // faixa de IPs reservada para documentação/testes
    private static final String IP_B = "203.0.113.20";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    @AfterEach
    void limparContadores() {
        redis.delete(CreateLinkRateLimitInterceptor.KEY_PREFIX + IP_A);
        redis.delete(CreateLinkRateLimitInterceptor.KEY_PREFIX + IP_B);
    }

    @Test
    void permiteDezCriacoesEBloqueiaADecimaPrimeiraCom429ERetryAfter() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(criarLinkDoIp(IP_A)).andExpect(status().isCreated());
        }

        MockHttpServletResponse response = mockMvc.perform(criarLinkDoIp(IP_A))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Muitas requisições"))
                .andReturn().getResponse();

        long retryAfter = Long.parseLong(response.getHeader("Retry-After"));
        assertThat(retryAfter).isBetween(1L, 60L);
    }

    @Test
    void oLimiteEhPorIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(criarLinkDoIp(IP_A)).andExpect(status().isCreated());
        }
        mockMvc.perform(criarLinkDoIp(IP_A)).andExpect(status().isTooManyRequests());

        // Outro IP continua podendo criar.
        mockMvc.perform(criarLinkDoIp(IP_B)).andExpect(status().isCreated());
    }

    @Test
    void contadorExpiraJuntoComAJanela() throws Exception {
        mockMvc.perform(criarLinkDoIp(IP_A)).andExpect(status().isCreated());

        Long ttlSeconds = redis.getExpire(CreateLinkRateLimitInterceptor.KEY_PREFIX + IP_A);
        assertThat(ttlSeconds).isBetween(1L, 60L);
    }

    @Test
    void naoLimitaOutrasRotas() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(criarLinkDoIp(IP_A)).andExpect(status().isCreated());
        }
        // Estourou o limite de criação, mas consultar estatísticas não é criação.
        mockMvc.perform(get("/api/links/naoExiste/stats").with(request -> {
                    request.setRemoteAddr(IP_A);
                    return request;
                }))
                .andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder criarLinkDoIp(String ip) {
        return post("/api/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"https://example.com\"}")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                });
    }
}
