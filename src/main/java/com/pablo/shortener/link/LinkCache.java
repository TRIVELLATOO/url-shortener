package com.pablo.shortener.link;

import com.pablo.shortener.config.AppProperties;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cache dos redirecionamentos no Redis, no padrão cache-aside: quem decide quando ler
 * e quando gravar no cache é o LinkService; o banco nem sabe que o cache existe.
 *
 * <p>O cache é uma otimização, não uma dependência: se o Redis falhar, os métodos
 * registram um aviso e se comportam como um "miss", e a aplicação segue pelo banco.
 *
 * <p>Hoje um link nunca muda nem é apagado, então não há invalidação. Quando existir
 * um endpoint de apagar/editar link, ele precisa remover a chave daqui.
 */
@Slf4j
@Component
public class LinkCache {

    private static final String KEY_PREFIX = "link:code:";

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final Duration ttl;

    public LinkCache(StringRedisTemplate redis, JsonMapper jsonMapper, AppProperties appProperties) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
        this.ttl = appProperties.cache().linkTtl();
    }

    public Optional<CachedLink> get(String code) {
        try {
            String json = redis.opsForValue().get(key(code));
            if (json == null) {
                log.debug("Cache MISS para o código {}", code);
                return Optional.empty();
            }
            log.debug("Cache HIT para o código {}", code);
            return Optional.of(jsonMapper.readValue(json, CachedLink.class));
        } catch (DataAccessException | JacksonException e) {
            log.warn("Falha ao ler o cache para o código {}; seguindo pelo banco: {}", code, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String code, CachedLink link) {
        try {
            // SET com expiração: o Redis apaga a chave sozinho quando o TTL acaba.
            redis.opsForValue().set(key(code), jsonMapper.writeValueAsString(link), ttl);
        } catch (DataAccessException | JacksonException e) {
            log.warn("Falha ao gravar o cache para o código {}: {}", code, e.getMessage());
        }
    }

    static String key(String code) {
        return KEY_PREFIX + code;
    }
}
