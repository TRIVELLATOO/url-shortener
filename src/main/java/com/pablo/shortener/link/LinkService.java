package com.pablo.shortener.link;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // o Lombok gera o construtor; o Spring injeta as dependências por ele
public class LinkService {

    private final LinkRepository linkRepository;
    private final ClickRepository clickRepository;
    private final LinkCache linkCache;

    /**
     * Insere o link sem código, usa o id gerado pelo banco para calcular o código Base62
     * e o preenche. Tudo na mesma transação: se algo falhar, nada fica gravado.
     */
    @Transactional
    public Link create(String originalUrl) {
        Link link = linkRepository.save(new Link(originalUrl));
        // Não precisa chamar save() de novo: o JPA detecta a mudança (dirty checking)
        // e faz o UPDATE sozinho quando a transação termina.
        link.setCode(Base62Encoder.encode(link.getId()));
        return link;
    }

    /**
     * Busca a URL original pelo código (primeiro no cache, depois no banco) e registra o clique.
     * O cache evita o SELECT; o INSERT do clique e o UPDATE do contador continuam acontecendo.
     */
    @Transactional
    public String resolve(String code) {
        CachedLink link = linkCache.get(code).orElseGet(() -> loadAndCache(code));

        // getReferenceById não vai ao banco: cria uma "referência" ao link só com o id.
        clickRepository.save(new Click(linkRepository.getReferenceById(link.id())));
        linkRepository.incrementClickCount(link.id());
        return link.originalUrl();
    }

    // Cache miss: busca no banco e guarda no Redis para os próximos acessos.
    private CachedLink loadAndCache(String code) {
        Link link = linkRepository.findByCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));
        CachedLink cached = new CachedLink(link.getId(), link.getOriginalUrl());
        linkCache.put(code, cached);
        return cached;
    }
}
