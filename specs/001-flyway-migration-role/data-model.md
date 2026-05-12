# Data Model: Flyway & User Role

## User Entity (extended)

The existing `User` entity gains a new `role` field.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | String (UUID) | PK, generated | No change |
| email | String | UNIQUE, nullable | No change |
| phone | String | UNIQUE, nullable | No change |
| username | String | UNIQUE, NOT NULL | No change |
| passwordHash | String | nullable | null for WeChat-only users |
| tokenVersion | int | NOT NULL, default 0 | No change |
| role | String | NOT NULL, default 'CUSTOMER_USER' | **NEW** |
| wechatOpenid | String | UNIQUE, nullable | No change |
| wechatUnionid | String | UNIQUE, nullable | No change |
| wechatOpenidQr | String | UNIQUE, nullable | No change |
| wechatNickname | String | nullable | No change |
| wechatAvatar | String | nullable | No change |
| createdAt | LocalDateTime | NOT NULL, updatable=false | No change |
| updatedAt | LocalDateTime | NOT NULL | No change |

### Validation Rules

- `role`: Must be a non-blank string, max 50 characters
- When a new user is created (register or wechatLogin), role defaults to `"CUSTOMER_USER"`

### State Transitions

- **Registration**: Account created → role set to `CUSTOMER_USER`
- **WeChat auto-registration**: Account created → role set to `CUSTOMER_USER`
- **Future**: Role may be updated by admin feature (out of scope)

## Device Entity (unchanged)

No changes to the Device entity. Included in the initial Flyway migration for completeness.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | String (UUID) | PK, generated | |
| userId | String | FK → users.id, NOT NULL | |
| deviceId | String | NOT NULL | |
| deviceType | enum (web, ios, android) | NOT NULL | Stored as VARCHAR |
| name | String | nullable | |
| lastActiveAt | LocalDateTime | NOT NULL | |
| createdAt | LocalDateTime | NOT NULL, updatable=false | |

## Flyway Migration Script (V1)

**File**: `src/main/resources/db/migration/V1__init_schema.sql`

```sql
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
```

## Configuration Changes

**application.yml** additions:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # Changed from create/update — Flyway manages schema
  flyway:
    enabled: true
    locations: classpath:db/migration
```
