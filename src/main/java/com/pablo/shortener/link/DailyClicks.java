package com.pablo.shortener.link;

import java.time.LocalDate;

/** Quantos cliques um link teve em um dia. */
public record DailyClicks(LocalDate date, long clicks) {
}
