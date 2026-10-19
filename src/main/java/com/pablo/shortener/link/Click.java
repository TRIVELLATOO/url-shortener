package com.pablo.shortener.link;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "clicks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Click {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: o Link só é buscado no banco se alguém chamar getLink() e usar seus dados.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "link_id", nullable = false, updatable = false)
    private Link link;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant clickedAt;

    public Click(Link link) {
        this.link = link;
    }
}
