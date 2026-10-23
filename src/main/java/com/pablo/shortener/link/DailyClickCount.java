package com.pablo.shortener.link;

import java.time.LocalDate;

/** Uma linha do resultado da consulta de cliques por dia (o Spring Data preenche pelos nomes das colunas). */
public interface DailyClickCount {

    LocalDate getDay();

    long getClicks();
}
