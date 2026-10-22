--liquibase formatted sql

--changeset catalog:001
CREATE TABLE users (
    id UUID PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    phone VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    work_day_start TIME,
    work_day_end TIME,
    CONSTRAINT ck_users_doctor_hours CHECK (
        (work_day_start IS NULL AND work_day_end IS NULL)
        OR (work_day_start IS NOT NULL AND work_day_end IS NOT NULL AND work_day_start < work_day_end)
    )
);

CREATE TABLE user_specializations (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    specialization VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, specialization)
);

CREATE TABLE dental_procedures (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL UNIQUE,
    description VARCHAR(2000),
    base_price NUMERIC(12, 2) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_procedure_price CHECK (base_price >= 0),
    CONSTRAINT ck_procedure_duration CHECK (duration_minutes > 0)
);

CREATE TABLE doctor_procedures (
    doctor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    procedure_id UUID NOT NULL REFERENCES dental_procedures(id) ON DELETE CASCADE,
    PRIMARY KEY (doctor_id, procedure_id)
);
