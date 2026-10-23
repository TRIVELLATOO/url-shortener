package com.pablo.shortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Informações exibidas no topo do Swagger UI (/swagger-ui.html). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shortenerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("URL Shortener API")
                .version("1.0")
                .description("Encurtador de URLs com Spring Boot, PostgreSQL e Redis: "
                        + "criação de links curtos, redirecionamento com contagem de cliques, "
                        + "estatísticas dos últimos 7 dias e rate limit por IP."));
    }
}
