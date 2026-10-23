package com.pablo.shortener.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.pablo.shortener.config.AppProperties;
import java.time.Duration;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private static final String CLOUDFLARE_EDGE = "172.71.195.7";

    @Test
    void semCabecalhoConfiguradoUsaOIpDaConexao() {
        MockHttpServletRequest request = requestFrom(CLOUDFLARE_EDGE);
        request.addHeader("CF-Connecting-IP", "203.0.113.50");

        assertThat(resolver("").resolve(request)).isEqualTo(CLOUDFLARE_EDGE);
    }

    @Test
    void comCabecalhoConfiguradoUsaOIpDoCabecalho() {
        MockHttpServletRequest request = requestFrom(CLOUDFLARE_EDGE);
        request.addHeader("CF-Connecting-IP", "203.0.113.50");

        assertThat(resolver("CF-Connecting-IP").resolve(request)).isEqualTo("203.0.113.50");
    }

    @Test
    void aceitaIpv6NoCabecalho() {
        MockHttpServletRequest request = requestFrom(CLOUDFLARE_EDGE);
        request.addHeader("CF-Connecting-IP", "2001:db8::1");

        assertThat(resolver("CF-Connecting-IP").resolve(request)).isEqualTo("2001:db8::1");
    }

    @Test
    void cabecalhoAusenteVoltaParaOIpDaConexao() {
        assertThat(resolver("CF-Connecting-IP").resolve(requestFrom(CLOUDFLARE_EDGE))).isEqualTo(CLOUDFLARE_EDGE);
    }

    @Test
    void valorQueNaoEhIpEhIgnorado() {
        MockHttpServletRequest request = requestFrom(CLOUDFLARE_EDGE);
        request.addHeader("CF-Connecting-IP", "abc:def; DROP *");

        assertThat(resolver("CF-Connecting-IP").resolve(request)).isEqualTo(CLOUDFLARE_EDGE);
    }

    @Test
    void listaDeIpsNoCabecalhoEhIgnorada() {
        // CF-Connecting-IP tem sempre um IP só; uma lista indica que não veio da Cloudflare.
        MockHttpServletRequest request = requestFrom(CLOUDFLARE_EDGE);
        request.addHeader("CF-Connecting-IP", "1.2.3.4, 5.6.7.8");

        assertThat(resolver("CF-Connecting-IP").resolve(request)).isEqualTo(CLOUDFLARE_EDGE);
    }

    @Test
    void validaFormatoDeIp() {
        assertThat(ClientIpResolver.isIpLiteral("203.0.113.50")).isTrue();
        assertThat(ClientIpResolver.isIpLiteral("::1")).isTrue();
        assertThat(ClientIpResolver.isIpLiteral("999.1.1.1")).isFalse();
        assertThat(ClientIpResolver.isIpLiteral("exemplo.com")).isFalse();
        assertThat(ClientIpResolver.isIpLiteral("")).isFalse();
    }

    @Test
    void logDeDiagnosticoNaoAtrapalhaAResolucao() {
        MockHttpServletRequest request = requestFrom(CLOUDFLARE_EDGE);
        request.addHeader("CF-Connecting-IP", "203.0.113.50");
        request.addHeader("X-Forwarded-For", "203.0.113.50, " + CLOUDFLARE_EDGE);

        ClientIpResolver resolver = new ClientIpResolver(props("CF-Connecting-IP", true));

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.50");
    }

    private static MockHttpServletRequest requestFrom(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/links");
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private static ClientIpResolver resolver(String header) {
        return new ClientIpResolver(props(header, false));
    }

    private static AppProperties props(String header, boolean logHeaders) {
        return new AppProperties("http://localhost:8080",
                new AppProperties.Cache(Duration.ofHours(24)),
                new AppProperties.Stats(ZoneId.of("America/Sao_Paulo")),
                new AppProperties.RateLimit(10, Duration.ofMinutes(1)),
                new AppProperties.ClientIp(header, logHeaders));
    }
}
