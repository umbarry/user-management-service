-- V1.0__Initial_Schema.sql
-- Initial database schema for User Management Service

CREATE TABLE users (
   id BIGSERIAL PRIMARY KEY,
   username VARCHAR(255) NOT NULL,
   email VARCHAR(255) NOT NULL UNIQUE,
   tax_code VARCHAR(16) NOT NULL UNIQUE,
   name VARCHAR(255) NOT NULL,
   surname VARCHAR(255) NOT NULL,
   status SMALLINT NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE,
   CONSTRAINT allowed_statuses CHECK (status IN (1, 2, 3))
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role SMALLINT NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT allowed_roles CHECK (role IN (1, 2, 3, 4, 5))
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_tax_code ON users(tax_code);


-- Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_type ON notifications(user_id, type);
CREATE INDEX idx_notifications_sent ON notifications(sent);

