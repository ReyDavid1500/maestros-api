--liquibase formatted sql

--changeset maestros:3 labels:setup
CREATE TABLE maestro_profiles (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID (),
    user_id UNIQUEIDENTIFIER NOT NULL,
    description NVARCHAR (1000),
    average_rating FLOAT NOT NULL DEFAULT 0.0,
    total_jobs INT NOT NULL DEFAULT 0,
    is_verified BIT NOT NULL DEFAULT 0,
    is_available BIT NOT NULL DEFAULT 1,
    created_at DATETIMEOFFSET NOT NULL,
    updated_at DATETIMEOFFSET NOT NULL,
    CONSTRAINT pk_maestro_profiles PRIMARY KEY (id),
    CONSTRAINT uq_maestro_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_maestro_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE maestro_services (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID (),
    maestro_profile_id UNIQUEIDENTIFIER NOT NULL,
    service_category_id UNIQUEIDENTIFIER NOT NULL,
    price_clp BIGINT NOT NULL,
    estimated_time NVARCHAR (50) NOT NULL,
    created_at DATETIMEOFFSET NOT NULL,
    updated_at DATETIMEOFFSET NOT NULL,
    CONSTRAINT pk_maestro_services PRIMARY KEY (id),
    CONSTRAINT uq_maestro_services_profile_category UNIQUE (
        maestro_profile_id,
        service_category_id
    ),
    CONSTRAINT fk_maestro_services_profile FOREIGN KEY (maestro_profile_id) REFERENCES maestro_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_maestro_services_category FOREIGN KEY (service_category_id) REFERENCES service_categories (id) ON DELETE NO ACTION
);