package com.pablo.shortener.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Testes do método que você vai escrever: ClickTimeline.fillMissingDays. */
class ClickTimelineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);

    @Test
    void semNenhumCliqueDevolveSeteDiasComZero() {
        List<DailyClicks> result = ClickTimeline.fillMissingDays(Map.of(), TODAY, 7);

        assertThat(result).containsExactly(
                new DailyClicks(LocalDate.of(2026, 10, 17), 0),
                new DailyClicks(LocalDate.of(2026, 10, 18), 0),
                new DailyClicks(LocalDate.of(2026, 10, 19), 0),
                new DailyClicks(LocalDate.of(2026, 10, 20), 0),
                new DailyClicks(LocalDate.of(2026, 10, 21), 0),
                new DailyClicks(LocalDate.of(2026, 10, 22), 0),
                new DailyClicks(LocalDate.of(2026, 10, 23), 0));
    }

    @Test
    void usaOsValoresDoMapaECompletaOsOutrosDiasComZero() {
        Map<LocalDate, Long> fromDb = Map.of(
                LocalDate.of(2026, 10, 21), 3L,
                LocalDate.of(2026, 10, 23), 2L);

        List<DailyClicks> result = ClickTimeline.fillMissingDays(fromDb, TODAY, 7);

        assertThat(result).containsExactly(
                new DailyClicks(LocalDate.of(2026, 10, 17), 0),
                new DailyClicks(LocalDate.of(2026, 10, 18), 0),
                new DailyClicks(LocalDate.of(2026, 10, 19), 0),
                new DailyClicks(LocalDate.of(2026, 10, 20), 0),
                new DailyClicks(LocalDate.of(2026, 10, 21), 3),
                new DailyClicks(LocalDate.of(2026, 10, 22), 0),
                new DailyClicks(LocalDate.of(2026, 10, 23), 2));
    }

    @Test
    void primeiroItemEhOMaisAntigoEUltimoEhHoje() {
        List<DailyClicks> result = ClickTimeline.fillMissingDays(Map.of(), TODAY, 7);

        assertThat(result.getFirst().date()).isEqualTo(TODAY.minusDays(6));
        assertThat(result.getLast().date()).isEqualTo(TODAY);
    }

    @Test
    void ignoraDiasDoMapaQueEstaoForaDaJanela() {
        Map<LocalDate, Long> fromDb = Map.of(
                LocalDate.of(2026, 10, 16), 99L,   // 1 dia antes da janela
                LocalDate.of(2026, 10, 20), 4L,
                LocalDate.of(2026, 10, 24), 99L);  // amanhã

        List<DailyClicks> result = ClickTimeline.fillMissingDays(fromDb, TODAY, 7);

        assertThat(result).hasSize(7);
        assertThat(result).extracting(DailyClicks::clicks).containsExactly(0L, 0L, 0L, 4L, 0L, 0L, 0L);
    }

    @Test
    void atravessaAViradaDeMesEDeAno() {
        List<DailyClicks> result = ClickTimeline.fillMissingDays(
                Map.of(LocalDate.of(2025, 12, 31), 5L), LocalDate.of(2026, 1, 3), 7);

        assertThat(result).extracting(DailyClicks::date).containsExactly(
                LocalDate.of(2025, 12, 28),
                LocalDate.of(2025, 12, 29),
                LocalDate.of(2025, 12, 30),
                LocalDate.of(2025, 12, 31),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 3));
        assertThat(result.get(3).clicks()).isEqualTo(5);
    }

    @Test
    void comUmDiaDevolveSoHoje() {
        List<DailyClicks> result = ClickTimeline.fillMissingDays(Map.of(TODAY, 8L), TODAY, 1);

        assertThat(result).containsExactly(new DailyClicks(TODAY, 8));
    }

    @Test
    void funcionaComOutraQuantidadeDeDias() {
        assertThat(ClickTimeline.fillMissingDays(Map.of(), TODAY, 30)).hasSize(30);
    }

    @Test
    void rejeitaQuantidadeDeDiasZero() {
        assertThatThrownBy(() -> ClickTimeline.fillMissingDays(Map.of(), TODAY, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaQuantidadeDeDiasNegativa() {
        assertThatThrownBy(() -> ClickTimeline.fillMissingDays(Map.of(), TODAY, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
