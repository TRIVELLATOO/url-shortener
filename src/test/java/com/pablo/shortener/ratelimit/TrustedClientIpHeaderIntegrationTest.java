package com.pablo.shortener.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Simula o cenário do Render: cada requisição chega por um servidor diferente da Cloudflare. */
@SpringBootTest(properties = "app.client-ip.header=CF-Connecting-IP")
@AutoConfigureMockMvc
@Transactional
class TrustedClientIpHeaderIntegrationTest {

    private static final String CLIENT = "203.0.113.77";
    private static final List<String> CLOUDFLARE_EDGES = List.of("172.71.195.7", "104.22.10.206", "172.70.35.105");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    @AfterEach
    void limpar() {
        redis.delete(CreateLinkRateLimitInterceptor.KEY_PREFIX + CLIENT);
        CLOUDFLARE_EDGES.forEach(ip -> redis.delete(CreateLinkRateLimitInterceptor.KEY_PREFIX + ip));
    }

    @Test
    void contaPeloIpRealMesmoComOServidorDaCloudflareMudandoACadaRequisicao() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(criarLink(CLOUDFLARE_EDGES.get(i % 3))).andExpect(status().isCreated());
        }

        // A 11ª criação, mesmo vindo por outro servidor da Cloudflare, é barrada.
        mockMvc.perform(criarLink(CLOUDFLARE_EDGES.get(1))).andExpect(status().isTooManyRequests());

        assertThat(redis.opsForValue().get(CreateLinkRateLimitInterceptor.KEY_PREFIX + CLIENT)).isEqualTo("11");
        CLOUDFLARE_EDGES.forEach(ip ->
                assertThat(redis.hasKey(CreateLinkRateLimitInterceptor.KEY_PREFIX + ip)).isFalse());
    }

    private MockHttpServletRequestBuilder criarLink(String edgeIp) {
        return post("/api/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\": \"https://example.com\"}")
                .header("CF-Connecting-IP", CLIENT)
                .with(request -> {
                    request.setRemoteAddr(edgeIp);
                    return request;
                });
    }
}
