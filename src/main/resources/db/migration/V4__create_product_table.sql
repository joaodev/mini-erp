CREATE TABLE product
(
    id         UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    name       VARCHAR(150)   NOT NULL,
    sku        VARCHAR(50)    NOT NULL,
    price      NUMERIC(19, 2) NOT NULL,
    active     BOOLEAN        NOT NULL DEFAULT TRUE,
    version    BIGINT         NOT NULL DEFAULT 0,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP      NULL
);

CREATE UNIQUE INDEX uk_product_sku
    ON product (sku)
    WHERE deleted_at IS NULL;