package com.pablo.shortener.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testa o endpoint de estatísticas com o relógio "parado" em 23/10/2026 12:00 UTC
 * (09:00 em São Paulo), para os dias esperados serem sempre os mesmos.
 * Janela esperada: 17/10 a 23/10, no fuso de São Paulo (UTC-3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LinkStatsIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ClickRepository clickRepository;

    private long linkId;

    @BeforeEach
    void criarLinkComCliques() {
        linkId = jdbcTemplate.queryForObject(
                "insert into links (code, original_url, click_count) values ('statsTeste', 'https://example.com', 5) returning id",
                Long.class);

        // 16/10 23:30 em SP (02:30 UTC do dia 17): está ANTES da janela e não pode ser contado.
        click("2026-10-17T02:30:00Z");
        // 17/10 00:30 em SP: é o 1º minuto útil da janela e precisa ser contado no dia 17.
        click("2026-10-17T03:30:00Z");
        // 20/10 23:00 em SP (02:00 UTC do dia 21): conta no dia 20, e não no 21.
        click("2026-10-21T02:00:00Z");
        click("2026-10-21T15:00:00Z");
        click("2026-10-21T18:00:00Z");
        click("2026-10-23T11:00:00Z");
    }

    @Test
    void consultaNoBancoComecaAMeiaNoiteDoFusoDasEstatisticas() {
        Instant from = Instant.parse("2026-10-17T03:00:00Z"); // meia-noite de 17/10 em São Paulo
        Instant to = Instant.parse("2026-10-24T03:00:00Z");

        List<DailyClickCount> rows = clickRepository.countClicksPerDay(linkId, "America/Sao_Paulo", from, to);

        assertThat(rows).extracting(DailyClickCount::getDay).containsExactly(
                LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 23));
        assertThat(rows).extracting(DailyClickCount::getClicks).containsExactly(1L, 1L, 2L, 1L);
    }

    @Test
    void devolveAsEstatisticasComOsSeteDias() throws Exception {
        mockMvc.perform(get("/api/links/statsTeste/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("statsTeste"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.totalClicks").value(5))
                .andExpect(jsonPath("$.clicksLast7Days.length()").value(7))
                .andExpect(jsonPath("$.clicksLast7Days[0].date").value("2026-10-17"))
                .andExpect(jsonPath("$.clicksLast7Days[0].clicks").value(1))
                .andExpect(jsonPath("$.clicksLast7Days[1].clicks").value(0))
                .andExpect(jsonPath("$.clicksLast7Days[2].clicks").value(0))
                .andExpect(jsonPath("$.clicksLast7Days[3].date").value("2026-10-20"))
                .andExpect(jsonPath("$.clicksLast7Days[3].clicks").value(1))
                .andExpect(jsonPath("$.clicksLast7Days[4].clicks").value(2))
                .andExpect(jsonPath("$.clicksLast7Days[5].clicks").value(0))
                .andExpect(jsonPath("$.clicksLast7Days[6].date").value("2026-10-23"))
                .andExpect(jsonPath("$.clicksLast7Days[6].clicks").value(1));
    }

    @Test
    void codigoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/links/naoExiste/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Link não encontrado"));
    }

    private void click(String instant) {
        jdbcTemplate.update("insert into clicks (link_id, clicked_at) values (?, ?)",
                linkId, Timestamp.from(Instant.parse(instant)));
    }
}
