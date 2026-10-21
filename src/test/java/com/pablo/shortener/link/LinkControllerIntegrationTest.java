package com.pablo.shortener.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sobe a aplicação inteira e conversa com o Postgres e o Redis do Docker Compose.
 * O @Transactional desfaz (rollback) tudo o que cada teste gravou no Postgres.
 * O Redis fica fora da transação, então as chaves criadas são apagadas no @AfterEach.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LinkControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redis;

    private final List<String> createdCodes = new ArrayList<>();

    @AfterEach
    void limparCache() {
        createdCodes.forEach(code -> redis.delete(LinkCache.key(code)));
    }

    @Test
    void criaLinkCurto() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://www.example.com/pagina\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("http://localhost:8080/[0-9a-zA-Z]{5,}")))
                .andExpect(jsonPath("$.code").value(matchesPattern("[0-9a-zA-Z]{5,}")))
                .andExpect(jsonPath("$.shortUrl").value(matchesPattern("http://localhost:8080/[0-9a-zA-Z]{5,}")))
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com/pagina"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void redirecionaERegistraOClique() throws Exception {
        // O 1º acesso é um cache MISS e o 2º é um HIT: os dois cliques precisam ser contados.
        String code = criarLink("https://www.example.com/destino");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.example.com/destino"));
        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound());

        Long clickCount = jdbcTemplate.queryForObject(
                "select click_count from links where code = ?", Long.class, code);
        Integer clicks = jdbcTemplate.queryForObject(
                "select count(*) from clicks c join links l on l.id = c.link_id where l.code = ?", Integer.class, code);
        assertThat(clickCount).isEqualTo(2);
        assertThat(clicks).isEqualTo(2);
    }

    @Test
    void primeiroAcessoGuardaOLinkNoCacheComTtlDe24h() throws Exception {
        String code = criarLink("https://www.example.com/cache");
        assertThat(redis.hasKey(LinkCache.key(code))).isFalse(); // cache-aside: criar não preenche o cache

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        assertThat(redis.opsForValue().get(LinkCache.key(code))).contains("https://www.example.com/cache");
        Long ttlSeconds = redis.getExpire(LinkCache.key(code));
        assertThat(ttlSeconds).isBetween(Duration.ofHours(24).minusMinutes(1).toSeconds(), Duration.ofHours(24).toSeconds());
    }

    @Test
    void segundoAcessoVemDoCacheENaoDoBanco() throws Exception {
        String code = criarLink("https://www.example.com/original");
        mockMvc.perform(get("/" + code)).andExpect(status().isFound()); // MISS: vai ao banco e guarda no Redis

        // Muda a URL direto no banco. Se o 2º acesso ainda devolver a antiga, quem respondeu foi o Redis.
        jdbcTemplate.update("update links set original_url = ? where code = ?", "https://www.example.com/alterada", code);

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.example.com/original"));
    }

    @Test
    void codigoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/naoExiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Link não encontrado"));
    }

    @Test
    void caminhoQueNaoEhCodigoNaoCaiNoRedirecionamento() throws Exception {
        // Tem ponto, então o regex da rota não aceita; quem responde é o tratamento padrão do Spring
        // para arquivo estático inexistente ("Not Found"), e não o nosso "Link não encontrado".
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void rejeitaUrlSemHttp() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").exists());
    }

    @Test
    void rejeitaUrlVazia() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").exists());
    }

    @Test
    void rejeitaUrlMaiorQue2048Caracteres() throws Exception {
        String url = "https://example.com/" + "a".repeat(2048);
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"" + url + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.url").exists());
    }

    @Test
    void rejeitaJsonMalformado() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{url"))
                .andExpect(status().isBadRequest());
    }

    private String criarLink(String url) throws Exception {
        String body = mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(body, "$.code");
        createdCodes.add(code);
        return code;
    }
}
