package com.pablo.shortener.link;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickRepository extends JpaRepository<Click, Long> {
}
