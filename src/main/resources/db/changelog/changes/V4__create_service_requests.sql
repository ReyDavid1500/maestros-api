--liquibase formatted sql

--changeset maestros:4 labels:setup
CREATE TABLE service_requests (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID (),
    client_id UNIQUEIDENTIFIER NOT NULL,
    maestro_id UNIQUEIDENTIFIER NOT NULL,
    service_category_id UNIQUEIDENTIFIER NOT NULL,
    description NVARCHAR (1000) NOT NULL,
    address_street NVARCHAR (200) NOT NULL,
    address_number NVARCHAR (20) NOT NULL,
    address_city NVARCHAR (100) NOT NULL,
    address_instructions NVARCHAR (500),
    scheduled_at DATETIMEOFFSET NOT NULL,
    payment_method NVARCHAR (20) NOT NULL DEFAULT 'CASH',
    status NVARCHAR (20) NOT NULL DEFAULT 'PENDING',
    accepted_at DATETIMEOFFSET,
    started_at DATETIMEOFFSET,
    completed_at DATETIMEOFFSET,
    cancelled_at DATETIMEOFFSET,
    created_at DATETIMEOFFSET NOT NULL,
    updated_at DATETIMEOFFSET NOT NULL,
    CONSTRAINT pk_service_requests PRIMARY KEY (id),
    CONSTRAINT chk_service_requests_status CHECK (
        status IN (
            'PENDING',
            'ACCEPTED',
            'IN_PROGRESS',
            'COMPLETED',
            'CANCELLED'
        )
    ),
    CONSTRAINT chk_service_requests_payment_method CHECK (payment_method IN ('CASH')),
    CONSTRAINT fk_service_requests_client FOREIGN KEY (client_id) REFERENCES users (id),
    CONSTRAINT fk_service_requests_maestro FOREIGN KEY (maestro_id) REFERENCES users (id),
    CONSTRAINT fk_service_requests_category FOREIGN KEY (service_category_id) REFERENCES service_categories (id)
);

CREATE INDEX idx_service_requests_client_id ON service_requests (client_id);

CREATE INDEX idx_service_requests_maestro_id ON service_requests (maestro_id);

CREATE INDEX idx_service_requests_status ON service_requests (status);