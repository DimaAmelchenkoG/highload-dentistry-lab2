--liquibase formatted sql

--changeset appointments:001
CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL,
    patient_name VARCHAR(200) NOT NULL,
    doctor_id UUID NOT NULL,
    doctor_name VARCHAR(200) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_appointment_interval CHECK (starts_at < ends_at)
);

CREATE INDEX ix_appointments_doctor_time ON appointments(doctor_id, starts_at, ends_at);
CREATE INDEX ix_appointments_patient_time ON appointments(patient_id, starts_at, ends_at);
CREATE INDEX ix_appointments_cursor ON appointments(created_at DESC, id DESC);

CREATE TABLE appointment_procedures (
    id UUID PRIMARY KEY,
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    procedure_id UUID NOT NULL,
    procedure_name VARCHAR(160) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    tooth_number INTEGER,
    CONSTRAINT ck_appointment_procedure_price CHECK (price >= 0),
    CONSTRAINT ck_appointment_procedure_quantity CHECK (quantity > 0),
    CONSTRAINT ck_tooth_number CHECK (tooth_number IS NULL OR tooth_number BETWEEN 1 AND 32)
);

CREATE INDEX ix_appointment_procedures_appointment ON appointment_procedures(appointment_id);
