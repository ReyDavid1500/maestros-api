--liquibase formatted sql

--changeset maestros:1 labels:setup
CREATE TABLE users (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID (),
    name NVARCHAR (100) NOT NULL,
    email NVARCHAR (255) NOT NULL,
    photo_url NVARCHAR (MAX),
    phone NVARCHAR (20),
    role NVARCHAR (20) NOT NULL,
    fcm_token NVARCHAR (MAX),
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIMEOFFSET NOT NULL,
    updated_at DATETIMEOFFSET NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT chk_users_role CHECK (role IN ('CLIENT', 'MAESTRO'))
);

CREATE UNIQUE INDEX idx_users_email ON users (email);