CREATE TABLE maestro_profiles (
    id UUID NOT NULL DEFAULT gen_random_uuid (),
    user_id UUID NOT NULL,
    description VARCHAR(1000),
    average_rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_jobs INTEGER NOT NULL DEFAULT 0,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_maestro_profiles PRIMARY KEY (id),
    CONSTRAINT uq_maestro_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_maestro_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE maestro_services (
    id UUID NOT NULL DEFAULT gen_random_uuid (),
    maestro_profile_id UUID NOT NULL,
    service_category_id UUID NOT NULL,
    price_clp BIGINT NOT NULL,
    estimated_time VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_maestro_services PRIMARY KEY (id),
    CONSTRAINT uq_maestro_services_profile_category UNIQUE (
        maestro_profile_id,
        service_category_id
    ),
    CONSTRAINT fk_maestro_services_profile FOREIGN KEY (maestro_profile_id) REFERENCES maestro_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_maestro_services_category FOREIGN KEY (service_category_id) REFERENCES service_categories (id) ON DELETE CASCADE
);