-- ByteBite Log Analysis AI Database Schema
-- PostgreSQL 15+ initialization script

-- Create schema
CREATE SCHEMA IF NOT EXISTS bytebite;
SET search_path TO bytebite;

-- Users/Credentials Table
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    ldap_dn VARCHAR(512),
    password_hash VARCHAR(512),
    is_active BOOLEAN DEFAULT true,
    is_admin BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- User Roles Table
CREATE TABLE user_roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_role_mapping (
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id INT NOT NULL REFERENCES user_roles(role_id) ON DELETE CASCADE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

-- Sessions Table
CREATE TABLE sessions (
    session_id VARCHAR(128) PRIMARY KEY,
    user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    session_data TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT true
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_expires_at ON sessions(expires_at);

-- Logs Table (for application logs)
CREATE TABLE application_logs (
    log_id BIGSERIAL PRIMARY KEY,
    level VARCHAR(20) NOT NULL,
    category VARCHAR(255),
    message TEXT,
    stack_trace TEXT,
    source_class VARCHAR(512),
    source_method VARCHAR(255),
    line_number INT,
    thread_name VARCHAR(255),
    user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    session_id VARCHAR(128) REFERENCES sessions(session_id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    request_id VARCHAR(128)
);

CREATE INDEX idx_logs_level ON application_logs(level);
CREATE INDEX idx_logs_category ON application_logs(category);
CREATE INDEX idx_logs_created_at ON application_logs(created_at DESC);
CREATE INDEX idx_logs_user_id ON application_logs(user_id);

-- Orders/Transactions Table (for E-commerce use case)
CREATE TABLE orders (
    order_id BIGSERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(50) DEFAULT 'PENDING',
    total_amount DECIMAL(10, 2),
    tax_amount DECIMAL(10, 2),
    shipping_amount DECIMAL(10, 2),
    shipping_address TEXT,
    billing_address TEXT,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_date ON orders(order_date DESC);

-- Order Items Table
CREATE TABLE order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    product_id VARCHAR(100),
    product_name VARCHAR(255),
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2),
    total_price DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- Analytics Events Table
CREATE TABLE analytics_events (
    event_id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB,
    user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    session_id VARCHAR(128) REFERENCES sessions(session_id) ON DELETE SET NULL,
    page_url VARCHAR(512),
    referrer_url VARCHAR(512),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_analytics_event_type ON analytics_events(event_type);
CREATE INDEX idx_analytics_created_at ON analytics_events(created_at DESC);
CREATE INDEX idx_analytics_user_id ON analytics_events(user_id);

-- KDS (Kitchen Display System) Orders Table
CREATE TABLE kds_orders (
    kds_order_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(order_id) ON DELETE CASCADE,
    kds_status VARCHAR(50) DEFAULT 'NEW',
    estimated_ready_time TIMESTAMP,
    actual_ready_time TIMESTAMP,
    priority_level INT DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kds_orders_status ON kds_orders(kds_status);
CREATE INDEX idx_kds_orders_priority ON kds_orders(priority_level);

-- API Audit Log Table
CREATE TABLE api_audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    api_endpoint VARCHAR(255),
    http_method VARCHAR(10),
    request_body TEXT,
    response_status INT,
    response_time_ms INT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_user_id ON api_audit_log(user_id);
CREATE INDEX idx_audit_log_created_at ON api_audit_log(created_at DESC);

-- Default Roles
INSERT INTO user_roles (role_name, description) VALUES
('ADMIN', 'Administrator with full access'),
('MANAGER', 'Manager with operational access'),
('STAFF', 'Staff member with limited access'),
('CUSTOMER', 'Customer role'),
('KDS_OPERATOR', 'Kitchen Display System operator');

-- Default Admin User (password should be changed!)
INSERT INTO users (username, email, is_active, is_admin) VALUES
('admin', 'admin@bytebite.local', true, true);

-- Assign admin role to admin user
INSERT INTO user_role_mapping (user_id, role_id) 
SELECT u.user_id, r.role_id FROM users u, user_roles r 
WHERE u.username = 'admin' AND r.role_name = 'ADMIN';

-- Grant permissions
GRANT USAGE ON SCHEMA bytebite TO bytebite;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA bytebite TO bytebite;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA bytebite TO bytebite;

COMMIT;
