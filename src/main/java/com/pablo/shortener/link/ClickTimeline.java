package com.pablo.shortener.link;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ClickTimeline {

    private ClickTimeline() {
    }

    /**
     * Monta a linha do tempo dos últimos {@code days} dias, terminando em {@code today}.
     *
     * <p>O banco só devolve os dias que tiveram clique; este método completa os dias que
     * faltam com 0.
     *
     * @param clicksByDay cliques por dia vindos do banco (só os dias com clique)
     * @param today       o último dia da janela (incluído)
     * @param days        quantos dias a lista deve ter; precisa ser maior que zero
     * @return exatamente {@code days} itens, do mais antigo ({@code today - (days - 1)}) até {@code today};
     *         cada dia usa o valor do mapa, ou 0 se não estiver nele. Dias do mapa fora da janela são ignorados.
     * @throws IllegalArgumentException se {@code days} for menor ou igual a zero
     */
    public static List<DailyClicks> fillMissingDays(Map<LocalDate, Long> clicksByDay, LocalDate today, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("A quantidade de dias deve ser maior que zero: " + days);
        }
        LocalDate firstDay = today.minusDays(days - 1);

        // Percorre a janela dia a dia (não o mapa): assim cada dia aparece uma vez, na ordem,
        // e dias do mapa fora da janela são ignorados naturalmente.
        List<DailyClicks> result = new ArrayList<>(days);
        for (int i = 0; i < days; i++) {
            LocalDate day = firstDay.plusDays(i);
            result.add(new DailyClicks(day, clicksByDay.getOrDefault(day, 0L)));
        }
        return result;
    }
}
