--liquibase formatted sql

--changeset maestros:2 labels:setup
CREATE TABLE service_categories (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID (),
    name NVARCHAR (100) NOT NULL,
    icon_name NVARCHAR (100) NOT NULL,
    display_order INT,
    created_at DATETIMEOFFSET NOT NULL,
    updated_at DATETIMEOFFSET NOT NULL,
    CONSTRAINT pk_service_categories PRIMARY KEY (id),
    CONSTRAINT uq_service_categories_name UNIQUE (name)
);

CREATE INDEX idx_service_categories_display_order ON service_categories (display_order);