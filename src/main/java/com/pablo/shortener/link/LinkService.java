package com.pablo.shortener.link;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // o Lombok gera o construtor; o Spring injeta os repositórios por ele
public class LinkService {

    private final LinkRepository linkRepository;
    private final ClickRepository clickRepository;

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

    /** Busca a URL original pelo código e registra o clique. */
    @Transactional
    public String resolve(String code) {
        Link link = linkRepository.findByCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));
        clickRepository.save(new Click(link));
        linkRepository.incrementClickCount(link.getId());
        return link.getOriginalUrl();
    }
}
