CREATE TABLE service_categories (
    id UUID NOT NULL DEFAULT gen_random_uuid (),
    name VARCHAR(100) NOT NULL,
    icon_name VARCHAR(100) NOT NULL,
    display_order INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_service_categories PRIMARY KEY (id),
    CONSTRAINT uq_service_categories_name UNIQUE (name)
);

CREATE INDEX idx_service_categories_display_order ON service_categories (display_order);