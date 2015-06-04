CREATE TABLE people (
    id VARCHAR(255) NOT NULL,
    historical_figure JSON,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified TIMESTAMP,

    CONSTRAINT people_pkey PRIMARY KEY (id)
);
