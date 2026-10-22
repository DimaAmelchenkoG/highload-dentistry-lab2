--liquibase formatted sql

--changeset catalog:002
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));
CREATE UNIQUE INDEX ux_dental_procedures_name_lower ON dental_procedures (lower(name));
