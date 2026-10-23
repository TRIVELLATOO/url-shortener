package com.pablo.shortener.link;

import com.pablo.shortener.config.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkStatsService {

    static final int DAYS = 7;

    private final LinkRepository linkRepository;
    private final ClickRepository clickRepository;
    private final AppProperties appProperties;
    private final Clock clock;

    // readOnly: avisa que esta transação só lê, o que permite algumas otimizações no Hibernate.
    @Transactional(readOnly = true)
    public LinkStatsResponse getStats(String code) {
        Link link = linkRepository.findByCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));

        ZoneId zone = appProperties.stats().zone();
        LocalDate today = LocalDate.ofInstant(clock.instant(), zone);

        // A janela começa à meia-noite do 1º dia NO FUSO das estatísticas (não em UTC)
        // e termina à meia-noite de amanhã, também nesse fuso.
        Instant from = today.minusDays(DAYS - 1).atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();

        Map<LocalDate, Long> clicksByDay = clickRepository
                .countClicksPerDay(link.getId(), zone.getId(), from, to).stream()
                .collect(Collectors.toMap(DailyClickCount::getDay, DailyClickCount::getClicks));

        List<DailyClicks> lastDays = ClickTimeline.fillMissingDays(clicksByDay, today, DAYS);

        return new LinkStatsResponse(link.getCode(), link.getOriginalUrl(), link.getCreatedAt(),
                link.getClickCount(), lastDays);
    }
}
