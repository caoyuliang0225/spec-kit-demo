CREATE TABLE users (
    id VARCHAR(36) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(255) UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    token_version INTEGER NOT NULL DEFAULT 0,
    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER_USER',
    wechat_openid VARCHAR(255) UNIQUE,
    wechat_unionid VARCHAR(255) UNIQUE,
    wechat_openid_qr VARCHAR(255) UNIQUE,
    wechat_nickname VARCHAR(255),
    wechat_avatar VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_wechat_openid ON users(wechat_openid);
CREATE INDEX idx_wechat_unionid ON users(wechat_unionid);
CREATE INDEX idx_wechat_openid_qr ON users(wechat_openid_qr);

CREATE TABLE devices (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    device_type VARCHAR(10) NOT NULL,
    name VARCHAR(255),
    last_active_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (user_id, device_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
