package com.pablo.shortener.link;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByCode(String code);

    // A soma acontece dentro do banco, então dois cliques simultâneos não "perdem" um incremento.
    @Modifying
    @Query("update Link l set l.clickCount = l.clickCount + 1 where l.id = :id")
    void incrementClickCount(@Param("id") Long id);
}
