CREATE TABLE service_requests (
    id UUID NOT NULL DEFAULT gen_random_uuid (),
    client_id UUID NOT NULL,
    maestro_id UUID NOT NULL,
    service_category_id UUID NOT NULL,
    description VARCHAR(1000) NOT NULL,
    address_street VARCHAR(200) NOT NULL,
    address_number VARCHAR(20) NOT NULL,
    address_city VARCHAR(100) NOT NULL,
    address_instructions VARCHAR(500),
    scheduled_at TIMESTAMPTZ NOT NULL,
    payment_method VARCHAR(20) NOT NULL DEFAULT 'CASH',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    accepted_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
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
    CONSTRAINT fk_service_requests_client FOREIGN KEY (client_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_service_requests_maestro FOREIGN KEY (maestro_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_service_requests_category FOREIGN KEY (service_category_id) REFERENCES service_categories (id) ON DELETE RESTRICT
);

CREATE INDEX idx_service_requests_client_id ON service_requests (client_id);

CREATE INDEX idx_service_requests_maestro_id ON service_requests (maestro_id);

CREATE INDEX idx_service_requests_status ON service_requests (status);