CREATE TABLE ratings (
    id UUID NOT NULL DEFAULT gen_random_uuid (),
    rater_id UUID NOT NULL,
    rated_id UUID NOT NULL,
    service_request_id UUID NOT NULL,
    score INTEGER NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_ratings PRIMARY KEY (id),
    CONSTRAINT chk_ratings_score CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT uq_ratings_rater_service_request UNIQUE (rater_id, service_request_id),
    CONSTRAINT fk_ratings_rater FOREIGN KEY (rater_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ratings_rated FOREIGN KEY (rated_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ratings_service_request FOREIGN KEY (service_request_id) REFERENCES service_requests (id) ON DELETE CASCADE
);

CREATE INDEX idx_ratings_rated_id ON ratings (rated_id);