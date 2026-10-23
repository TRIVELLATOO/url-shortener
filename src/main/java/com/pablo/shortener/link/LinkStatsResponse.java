package com.pablo.shortener.link;

import java.time.Instant;
import java.util.List;

public record LinkStatsResponse(
        String code,
        String originalUrl,
        Instant createdAt,
        long totalClicks,
        List<DailyClicks> clicksLast7Days) {
}
