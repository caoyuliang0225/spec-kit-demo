CREATE TABLE roles (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    PRIMARY KEY (id)
);

INSERT INTO roles (id, name, description)
VALUES (gen_random_uuid(), 'CUSTOMER_USER', 'Default role for registered customers');

CREATE TABLE user_roles (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

INSERT INTO user_roles (id, user_id, role_id, assigned_at)
SELECT gen_random_uuid(), u.id, r.id, u.created_at
FROM users u
CROSS JOIN roles r
WHERE r.name = COALESCE(NULLIF(u.role, ''), 'CUSTOMER_USER');

ALTER TABLE users DROP COLUMN role;
