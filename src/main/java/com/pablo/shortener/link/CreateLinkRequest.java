package com.pablo.shortener.link;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateLinkRequest(
        @NotBlank(message = "A URL é obrigatória")
        @Size(max = 2048, message = "A URL deve ter no máximo 2048 caracteres")
        @URL(message = "A URL não é válida")
        // Só http/https: impede encurtar coisas como "javascript:alert(1)".
        @Pattern(regexp = "(?i)^https?://\\S+$", message = "A URL deve começar com http:// ou https://")
        String url) {
}
