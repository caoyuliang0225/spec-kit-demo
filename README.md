# Portal Auth

A unified authentication portal for Web and Mobile apps supporting registration, login, and logout via email or phone number, with password and OTP authentication methods.

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.4 |
| Database | PostgreSQL |
| Cache/Store | Redis |
| Auth | JWT (jjwt 0.12.5) + BCrypt |
| Build | Maven |

## Quick Start

```bash
# Prerequisites: Java 17, PostgreSQL, Redis

# Clone and configure
cp src/main/resources/application.yml src/main/resources/application-local.yml
# Edit application-local.yml with your DB/Redis credentials

# Run
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Test
./mvnw test
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | (default) | HMAC-SHA key for JWT signing |
| `CAPTCHA_SECRET` | (empty) | CAPTCHA provider secret key |
| `MAIL_HOST` | `localhost` | SMTP host |
| `MAIL_PORT` | `25` | SMTP port |

## Configuration Reference

All configurable in `application.yml`:

```yaml
app:
  jwt:
    access-token-expiration: 900000       # 15 min in ms
    refresh-token-expiration: 604800000   # 7 days in ms
  otp:
    length: 6
    ttl-seconds: 300                      # 5 min
    resend-cooldown-seconds: 60           # 1 min
    max-verify-attempts: 5
    max-send-per-hour: 5
  rate-limit:
    qps-per-endpoint: 100
    login-max-attempts: 10
    login-window-seconds: 300             # 5 min
  device:
    active-window-minutes: 10
    max-active-devices: 2
  captcha:
    provider: turnstile                    # turnstile | recaptcha | hcaptcha
    secret-key: ${CAPTCHA_SECRET:}
```

## API Reference

### Auth Endpoints (no auth required)

All auth endpoints are prefixed with `/api/auth`.

### `POST /api/auth/send-otp`

Send OTP to email or phone after CAPTCHA verification.

**Request:**
```json
{
  "account": "user@example.com",
  "captchaToken": "cloudflare-turnstile-token"
}
```

**Response:** `200 OK`

**Errors:** `AUTH_002` (CAPTCHA failed), `AUTH_007` (rate limited)

---

### `POST /api/auth/verify-otp`

Verify OTP code and receive a temporary token for registration or password reset.

**Query params:** `purpose=register` (default) or `purpose=reset_password`

**Request:**
```json
{
  "account": "user@example.com",
  "otpCode": "123456"
}
```

**Response:** `200 OK`
```json
{
  "tempToken": "eyJhbGciOiJIUzI1NiJ9...",
  "purpose": "register"
}
```

**Errors:** `AUTH_001` (invalid/expired OTP)

---

### `POST /api/auth/register`

Create a new account using the temporary token from OTP verification.

**Request:**
```json
{
  "tempToken": "eyJhbGciOiJIUzI1NiJ9...",
  "password": "securePassword123",
  "username": "myusername"
}
```

**Response:** `201 Created`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "uuid-string",
  "expiresIn": 900000
}
```

**Errors:** `AUTH_004` (email/phone/username taken)

---

### `POST /api/auth/login`

Authenticate with password or OTP.

**Request:**
```json
{
  "account": "user@example.com",
  "credential": "myPassword123",
  "type": "pwd",
  "deviceId": "browser-uuid",
  "deviceType": "web",
  "deviceName": "Chrome on macOS"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "uuid-string",
  "expiresIn": 900000
}
```

**Errors:** `AUTH_003` (not found), `AUTH_005` (wrong password), `AUTH_001` (wrong OTP), `AUTH_007` (rate limited)

---

### `POST /api/auth/forgot-password`

Initiate password reset by sending OTP.

**Request:**
```json
{
  "account": "user@example.com",
  "captchaToken": "cloudflare-turnstile-token"
}
```

**Response:** `200 OK`

**Errors:** `AUTH_003` (not found)

---

### `POST /api/auth/reset-password`

Complete password reset using the temporary token from OTP verification.

**Request:**
```json
{
  "tempToken": "eyJhbGciOiJIUzI1NiJ9...",
  "newPassword": "newSecurePassword456"
}
```

**Response:** `200 OK` (new JWT pair, all other sessions invalidated)

---

### `POST /api/auth/refresh`

Get a new access token using a refresh token.

**Request:**
```json
{
  "refreshToken": "uuid-string"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "new-uuid-string",
  "expiresIn": 900000
}
```

**Errors:** `AUTH_008` (invalid/expired refresh token)

---

### `POST /api/auth/logout`

Revoke a refresh token.

**Request:**
```json
{
  "refreshToken": "uuid-string"
}
```

**Response:** `200 OK`

---

### Device Endpoints (auth required)

All device endpoints require `Authorization: Bearer <access_token>` header.

### `POST /api/devices/heartbeat`

Update device last active timestamp.

**Request body:** `deviceId` (plain string, not JSON)

---

### `GET /api/devices`

List all devices for the authenticated user.

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "deviceId": "browser-uuid",
    "deviceType": "web",
    "name": "Chrome on macOS",
    "lastActiveAt": "2026-05-11T10:30:00",
    "createdAt": "2026-05-10T08:00:00"
  }
]
```

---

### `DELETE /api/devices/{deviceId}`

Remove a device.

**Response:** `200 OK`

## Flows

### Registration Flow

```
Client                    Server                      Redis/DB
  │                         │                          │
  ├─ POST /send-otp ───────→│                          │
  │   {account, captcha}    │─────────────────────────→│ Check CAPTCHA
  │                         │←─────────────────────────│
  │                         │── Generate OTP ─────────→│ Store OTP (5min TTL)
  │                         │── Send email/SMS         │
  │◀──────── 200 ──────────│                          │
  │                         │                          │
  ├─ POST /verify-otp ─────→│                          │
  │   {account, code}       │─────────────────────────→│ Verify OTP
  │                         │←─────────────────────────│
  │◀─── {tempToken} ───────│                          │
  │                         │                          │
  ├─ POST /register ───────→│                          │
  │   {tempToken, pwd, un}  │── Validate tempToken     │
  │                         │── Hash password          │
  │                         │── Create User ──────────→│ DB
  │                         │── Issue JWT pair         │
  │◀─── {access,refresh} ──│                          │
```

### Login Flow

```
Client                    Server                      Redis/DB
  │                         │                          │
  ├─ POST /login ──────────→│                          │
  │   {account,cred,type}   │                          │
  │                         │── type=pwd?              │
  │                         │   ├─ Verify bcrypt ─────→│ DB
  │                         │── type=otp?              │
  │                         │   ├─ Verify OTP ────────→│ Redis
  │                         │                          │
  │                         │── Check device limit     │
  │                         │── ≥2 active? → kick      │
  │                         │── Issue JWT (w/ version) │
  │◀─── {access,refresh} ──│                          │
```

### Device Kick-out on Concurrent Login

```
Device A (old)         Device B (active)        Server          Device C (new)
  │                         │                    │                 │
  │                         │                    │←─ login ───────│
  │                         │                    │                 │
  │                         │                    │── Check active  │
  │                         │                    │── Count: 2      │
  │                         │                    │── Kick Device A │
  │                         │                    │── user.version++│
  │                         │                    │── Issue JWT v2  │
  │                         │                    │──→ OK           │
  │                         │                    │                 │
  │─ request (JWT v1) ─────→│                    │                 │
  │                         │── TokenVersionFilter                │
  │                         │── 1 < 2 → 401      │                 │
  │◀──── 401 (kicked) ─────│                    │                 │
```

## Rate Limiting

| Layer | Scope | Limit |
|---|---|---|
| QPS per endpoint | endpoint key | 100 QPS (Redis sliding window) |
| Send OTP | account | 5/hour |
| Verify OTP | account | 5/5min |
| Login | account + IP | 10/5min |

## Error Codes

| Code | Meaning | HTTP Status |
|---|---|---|
| `AUTH_001` | Invalid/expired OTP | 401 |
| `AUTH_002` | CAPTCHA verification failed | 401 |
| `AUTH_003` | Account not found | 409 |
| `AUTH_004` | Account already exists | 409 |
| `AUTH_005` | Wrong password | 401 |
| `AUTH_006` | Device limit exceeded (auto-kick) | 403 |
| `AUTH_007` | Rate limited | 429 |
| `AUTH_008` | Invalid/expired token | 401 |
| `AUTH_009` | Token version mismatch (kicked) | 401 |
| `AUTH_400` | Validation error | 400 |
| `AUTH_500` | Internal server error | 500 |

## Architecture

```
                  ┌─────────────────────┐
                  │   Client (Web/App)  │
                  └────────┬────────────┘
                           │
                  ┌────────▼────────────┐
                  │   RateLimitInterceptor│  ← 100 QPS
                  └────────┬────────────┘
                           │
                  ┌────────▼────────────┐
                  │  JwtAuthFilter      │  ← Validate access token
                  └────────┬────────────┘
                           │
                  ┌────────▼────────────┐
                  │  TokenVersionFilter │  ← Check kick status
                  └────────┬────────────┘
                           │
                  ┌────────▼────────────┐
                  │  AuthController     │
                  │  DeviceController   │
                  └────────┬────────────┘
                           │
                  ┌────────▼────────────┐
                  │  AuthService        │  ← Orchestrates flows
                  │  OtpService         │
                  │  TokenService       │
                  │  DeviceService      │
                  │  RateLimitService   │
                  │  CaptchaService     │
                  └────────┬────────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │PostgreSQL│ │  Redis   │ │ SMTP/SMS │
        └──────────┘ └──────────┘ └──────────┘
```
