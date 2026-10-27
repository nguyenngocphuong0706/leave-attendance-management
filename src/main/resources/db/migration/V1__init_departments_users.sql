CREATE TABLE departments (
                             id   BIGSERIAL PRIMARY KEY,
                             name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       email         VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name     VARCHAR(100) NOT NULL,
                       role          VARCHAR(20)  NOT NULL,
                       department_id BIGINT REFERENCES departments(id),
                       manager_id    BIGINT REFERENCES users(id),
                       active        BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);