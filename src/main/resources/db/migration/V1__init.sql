CREATE TABLE links (
    id           BIGSERIAL    PRIMARY KEY,
    -- Sem NOT NULL: o link é inserido primeiro e o código (Base62 do id) é preenchido logo depois.
    -- No PostgreSQL, UNIQUE permite vários NULLs, então isso não gera conflito.
    code         VARCHAR(16)  UNIQUE,
    original_url TEXT         NOT NULL,
    click_count  BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE clicks (
    id         BIGSERIAL   PRIMARY KEY,
    link_id    BIGINT      NOT NULL REFERENCES links (id) ON DELETE CASCADE,
    clicked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_clicks_link_id_clicked_at ON clicks (link_id, clicked_at);
