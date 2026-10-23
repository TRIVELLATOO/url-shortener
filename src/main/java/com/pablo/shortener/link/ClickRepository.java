package com.pablo.shortener.link;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickRepository extends JpaRepository<Click, Long> {

    /**
     * Conta os cliques de um link por dia, no fuso informado, dentro de [from, to).
     * Só devolve os dias que tiveram clique. O filtro por link_id + clicked_at usa o índice
     * idx_clicks_link_id_clicked_at criado na V1.
     *
     * <p>É uma query nativa (SQL do Postgres) porque o JPQL não tem "AT TIME ZONE".
     */
    @Query(value = """
            SELECT CAST(clicked_at AT TIME ZONE :zone AS date) AS day,
                   COUNT(*)                                     AS clicks
            FROM clicks
            WHERE link_id = :linkId
              AND clicked_at >= :from
              AND clicked_at <  :to
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<DailyClickCount> countClicksPerDay(@Param("linkId") Long linkId,
                                            @Param("zone") String zone,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);
}
