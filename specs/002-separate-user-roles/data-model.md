# Data Model: Separate User Role Table

## Role Entity (new)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | String (UUID) | PK, generated | |
| name | String | UNIQUE, NOT NULL | e.g., CUSTOMER_USER, ADMIN |
| description | String | nullable | Human-readable role description |

### Seed Data

| name | description |
|------|-------------|
| CUSTOMER_USER | Default role for registered customers |

## UserRole (junction, new)

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | String (UUID) | PK, generated | |
| userId | String | FK → users.id, NOT NULL | |
| roleId | String | FK → roles.id, NOT NULL | |
| assignedAt | LocalDateTime | NOT NULL | Set automatically on creation |
| UNIQUE(userId, roleId) | | | Prevents duplicate assignments |

## User Entity (modified)

**Removed**:
- `role` field and column (migrated to user_roles table)

**Added**:
- `Set<Role> roles` — `@ManyToMany` mapped through user_roles, or
- `List<UserRole> userRoles` — explicit one-to-many via UserRole entity

## Flyway Migration (V2)

**File**: `src/main/resources/db/migration/V2__separate_user_roles.sql`

```sql
-- 1. Create roles lookup table
CREATE TABLE roles (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    PRIMARY KEY (id)
);

-- 2. Seed the default role
INSERT INTO roles (id, name, description)
VALUES (gen_random_uuid(), 'CUSTOMER_USER', 'Default role for registered customers');

-- 3. Create user_roles junction table
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

-- 4. Migrate existing role data
INSERT INTO user_roles (id, user_id, role_id, assigned_at)
SELECT gen_random_uuid(), u.id, r.id, u.created_at
FROM users u
CROSS JOIN roles r
WHERE r.name = COALESCE(NULLIF(u.role, ''), 'CUSTOMER_USER');

-- 5. Drop old column
ALTER TABLE users DROP COLUMN role;
```

## Service Logic Changes

**AuthService.register()** — replace `user.setRole("CUSTOMER_USER")` with:
```java
Role customerRole = roleRepository.findByName("CUSTOMER_USER")
    .orElseThrow(() -> new RuntimeException("Default role not found"));
UserRole userRole = new UserRole();
userRole.setUser(user);
userRole.setRole(customerRole);
userRoleRepository.save(userRole);
```

**AuthService.wechatLogin()** — same replacement for the new user creation path.

## Modified Files

| File | Action | Reason |
|------|--------|--------|
| `model/Role.java` | CREATE | New entity for roles table |
| `model/UserRole.java` | CREATE | Junction entity |
| `repository/RoleRepository.java` | CREATE | findByName query |
| `repository/UserRoleRepository.java` | CREATE | Standard CRUD |
| `model/User.java` | MODIFY | Remove role field, add relationship |
| `service/AuthService.java` | MODIFY | Use repository instead of setRole |
| `V2__separate_user_roles.sql` | CREATE | Migration script |
| `AuthServiceTest.java` | MODIFY | Update role assertion |
