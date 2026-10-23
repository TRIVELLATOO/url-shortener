package com.pablo.shortener.link;

import com.pablo.shortener.config.AppProperties;
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

@RestController
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;
    private final LinkStatsService linkStatsService;
    private final AppProperties appProperties;

    @PostMapping("/api/links")
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        Link link = linkService.create(request.url());
        LinkResponse response = LinkResponse.from(link, appProperties.baseUrl());
        // 201 Created + cabeçalho Location apontando para o recurso criado.
        return ResponseEntity.created(URI.create(response.shortUrl())).body(response);
    }

    // Não passa pelo cache nem conta como clique: ver as estatísticas não é acessar o link.
    @GetMapping("/api/links/{code}/stats")
    public LinkStatsResponse stats(@PathVariable String code) {
        return linkStatsService.getStats(code);
    }

    // O regex limita a rota a códigos Base62: /favicon.ico, /index.html etc. não caem aqui.
    @GetMapping("/{code:[0-9a-zA-Z]{1,16}}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String originalUrl = linkService.resolve(code);
        // 302 (e não 301): o navegador não guarda o redirecionamento em cache,
        // então todo acesso passa pelo servidor e o clique é contado.
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }
}
