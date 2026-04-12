-- V1.1__Create_Admin_User.sql
-- Insert initial admin user

INSERT INTO users (username, email, tax_code, name, surname, status, created_at, updated_at)
VALUES ('admin', 'admin@umbarry.dev', 'ADMUSR80A01H501U', 'Admin', 'User', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_roles (user_id, role)
SELECT id, 1 FROM users WHERE email = 'admin@umbarry.dev';
