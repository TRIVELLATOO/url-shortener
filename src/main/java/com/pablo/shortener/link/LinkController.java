package com.pablo.shortener.link;

import com.pablo.shortener.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Links", description = "Criação, redirecionamento e estatísticas de links curtos")
@RestController
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;
    private final LinkStatsService linkStatsService;
    private final AppProperties appProperties;

    @Operation(summary = "Cria um link curto")
    @ApiResponse(responseCode = "201", description = "Link criado")
    @ApiResponse(responseCode = "400", description = "URL inválida")
    @ApiResponse(responseCode = "429", description = "Limite de criações por IP atingido (ver cabeçalho Retry-After)")
    @PostMapping("/api/links")
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        Link link = linkService.create(request.url());
        LinkResponse response = LinkResponse.from(link, appProperties.baseUrl());
        // 201 Created + cabeçalho Location apontando para o recurso criado.
        return ResponseEntity.created(URI.create(response.shortUrl())).body(response);
    }

    // Não passa pelo cache nem conta como clique: ver as estatísticas não é acessar o link.
    @Operation(summary = "Estatísticas do link: total de cliques e cliques por dia nos últimos 7 dias")
    @ApiResponse(responseCode = "200", description = "Estatísticas do link")
    @ApiResponse(responseCode = "404", description = "Código não encontrado")
    @GetMapping("/api/links/{code}/stats")
    public LinkStatsResponse stats(@Parameter(description = "Código do link", example = "6LAzf") @PathVariable String code) {
        return linkStatsService.getStats(code);
    }

    // O regex limita a rota a códigos Base62: /favicon.ico, /index.html etc. não caem aqui.
    @Operation(summary = "Redireciona para a URL original e registra o clique",
            description = "Responde 302 com o cabeçalho Location. Abra no navegador para ser redirecionado.")
    @ApiResponse(responseCode = "302", description = "Redirecionamento para a URL original")
    @ApiResponse(responseCode = "404", description = "Código não encontrado")
    @GetMapping("/{code:[0-9a-zA-Z]{1,16}}")
    public ResponseEntity<Void> redirect(@Parameter(description = "Código do link", example = "6LAzf") @PathVariable String code) {
        String originalUrl = linkService.resolve(code);
        // 302 (e não 301): o navegador não guarda o redirecionamento em cache,
        // então todo acesso passa pelo servidor e o clique é contado.
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }
}
