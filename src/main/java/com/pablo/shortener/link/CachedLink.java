package com.pablo.shortener.link;

/**
 * O que fica guardado no Redis para cada código.
 * Guardamos o id, além da URL, para registrar o clique sem precisar buscar o link no banco.
 */
public record CachedLink(long id, String originalUrl) {
}
