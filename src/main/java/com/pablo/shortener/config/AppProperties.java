package com.pablo.shortener.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configurações próprias da aplicação, lidas do prefixo "app" no application.yml. */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotBlank String baseUrl) {

    public AppProperties {
        // Remove a barra final para não gerar "http://localhost:8080//abc".
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }
}
