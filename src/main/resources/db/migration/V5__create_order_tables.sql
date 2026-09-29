CREATE TABLE orders
(
    id           UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    customer_id  UUID           NOT NULL REFERENCES customer (id),
    status       VARCHAR(20)    NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    version      BIGINT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP      NULL
);

CREATE TABLE order_item
(
    id         UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    order_id   UUID           NOT NULL REFERENCES orders (id),
    product_id UUID           NOT NULL REFERENCES product (id),
    quantity   INTEGER        NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    subtotal   NUMERIC(19, 2) NOT NULL,
    version    BIGINT         NOT NULL DEFAULT 0,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP      NULL
);

CREATE INDEX idx_order_item_order_id ON order_item (order_id);