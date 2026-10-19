package com.pablo.shortener.link;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // o JPA exige um construtor vazio; protected evita uso no resto do código
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // o id vem do BIGSERIAL do Postgres
    private Long id;

    // Começa nulo e é preenchido logo após o insert, a partir do id (ver LinkService#create).
    @Setter
    @Column(unique = true, length = 16)
    private String code;

    @Column(nullable = false, columnDefinition = "text")
    private String originalUrl;

    // Só é alterado por UPDATE atômico no banco (LinkRepository#incrementClickCount).
    @Column(nullable = false, updatable = false)
    private long clickCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Link(String originalUrl) {
        this.originalUrl = originalUrl;
    }
}
