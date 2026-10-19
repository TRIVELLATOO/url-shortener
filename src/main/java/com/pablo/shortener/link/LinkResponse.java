package com.pablo.shortener.link;

import java.time.Instant;

public record LinkResponse(String code, String shortUrl, String originalUrl, Instant createdAt) {

    static LinkResponse from(Link link, String baseUrl) {
        return new LinkResponse(link.getCode(), baseUrl + "/" + link.getCode(), link.getOriginalUrl(), link.getCreatedAt());
    }
}
