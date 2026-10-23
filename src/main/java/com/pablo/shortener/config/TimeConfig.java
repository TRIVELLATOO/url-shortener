package com.pablo.shortener.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    /**
     * O "relógio" da aplicação. Quem precisa saber que horas são recebe este Clock
     * em vez de chamar Instant.now() direto, e assim os testes podem trocá-lo por um relógio parado.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
