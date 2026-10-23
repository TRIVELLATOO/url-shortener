package com.pablo.shortener.ratelimit;

import com.pablo.shortener.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Descobre o IP real de quem fez a requisição.
 *
 * <p>Se {@code app.client-ip.header} estiver configurado (ex.: CF-Connecting-IP), usa esse cabeçalho.
 * Isso só é seguro quando TODA requisição passa por um proxy que sobrescreve o cabeçalho, como
 * a Cloudflare na frente do Render: um cliente que mande o próprio CF-Connecting-IP tem o valor
 * substituído pela Cloudflare. Fora desse cenário, deixe a configuração vazia.
 *
 * <p>Sem cabeçalho configurado (ou se ele vier ausente ou inválido), usa {@code getRemoteAddr()},
 * que o Tomcat já ajusta pelo X-Forwarded-For quando o proxy é de rede interna.
 */
@Slf4j
@Component
public class ClientIpResolver {

    // Só para o log temporário de diagnóstico.
    private static final List<String> DIAGNOSTIC_HEADERS = List.of(
            "X-Forwarded-For", "CF-Connecting-IP", "True-Client-IP", "X-Real-IP", "Forwarded", "CF-Ray");

    // Validação simples de formato: o valor vira parte de uma chave no Redis,
    // então não aceitamos qualquer texto vindo de um cabeçalho.
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private final String trustedHeader;
    private final boolean logHeaders;

    public ClientIpResolver(AppProperties appProperties) {
        this.trustedHeader = appProperties.clientIp().header();
        this.logHeaders = appProperties.clientIp().logHeaders();
    }

    public String resolve(HttpServletRequest request) {
        if (logHeaders) {
            logDiagnosticHeaders(request);
        }
        if (StringUtils.hasText(trustedHeader)) {
            String value = request.getHeader(trustedHeader);
            if (value != null && isIpLiteral(value.trim())) {
                return value.trim();
            }
            log.warn("Cabeçalho {} ausente ou inválido ({}); usando o IP da conexão {}",
                    trustedHeader, value, request.getRemoteAddr());
        }
        return request.getRemoteAddr();
    }

    static boolean isIpLiteral(String value) {
        if (IPV4.matcher(value).matches()) {
            for (String part : value.split("\\.")) {
                if (Integer.parseInt(part) > 255) {
                    return false;
                }
            }
            return true;
        }
        return value.contains(":") && IPV6.matcher(value).matches();
    }

    private void logDiagnosticHeaders(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder("Diagnóstico de IP | remoteAddr=").append(request.getRemoteAddr());
        for (String name : DIAGNOSTIC_HEADERS) {
            sb.append(" | ").append(name).append('=').append(request.getHeader(name));
        }
        log.info(sb.toString());
    }
}
