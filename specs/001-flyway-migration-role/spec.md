# Feature Specification: Flyway Database Migration & Default User Role

**Feature Branch**: `001-flyway-migration-role`
**Created**: 2026-05-12
**Status**: Draft
**Input**: User description: "现在要求数据库的建表语句要使用flyway进行管理。可以根据现在代码里现有的代码生成对应的sql语句，然后写在flyway脚本里。新需求：当用户注册时，会给用户一个默认的role，值是：CUSTOMER_USER。"

## User Scenarios & Testing

### User Story 1 - Database schema managed through Flyway migrations (Priority: P1)

As a developer setting up the project for the first time or deploying to a new environment, I want all database tables to be created through Flyway migration scripts, so that the database schema is version-controlled, reproducible across environments, and evolves in a documented manner.

**Why this priority**: This is the foundational requirement — without Flyway in place, there is no migration mechanism to add the role column or manage future schema changes.

**Independent Test**: A developer can run the application against a fresh PostgreSQL database, verify that the `users` and `devices` tables are automatically created with the correct schema, and confirm that `flyway_schema_history` is populated.

**Acceptance Scenarios**:

1. **Given** an empty PostgreSQL database configured for the application, **When** the application starts, **Then** the `users` and `devices` tables are automatically created with all columns matching the existing JPA entity definitions.
2. **Given** the application has been started once, **When** it restarts, **Then** no migrations re-run (idempotent startup) and no errors are reported.
3. **Given** a running application, **When** a developer inspects the database, **Then** the `flyway_schema_history` table exists and records the initial migration.

---

### User Story 2 - Default role assigned to new user registrations (Priority: P1)

As a new user registering on the platform, I am automatically assigned the CUSTOMER_USER role upon successful account creation, so that I have the appropriate baseline permissions without needing an administrator to manually assign them.

**Why this priority**: This is the core business requirement — it directly affects the registration flow and user permission model.

**Independent Test**: A new user completes registration via email or phone, and their user record in the database shows the role value `CUSTOMER_USER`.

**Acceptance Scenarios**:

1. **Given** a new user submits a valid registration request with email, username, and password, **When** the account is created, **Then** the user record stores role `CUSTOMER_USER`.
2. **Given** a new user submits a valid registration request with phone number instead of email, **When** the account is created, **Then** the user record stores role `CUSTOMER_USER`.
3. **Given** a user registers via WeChat OAuth (scan or redirect), **When** the account is auto-created, **Then** the user record stores role `CUSTOMER_USER`.

---

### User Story 3 - Existing database schema represented as initial Flyway migration (Priority: P2)

As a developer maintaining the project, I want the existing `users` and `devices` table definitions captured in the initial Flyway migration script, so that the current schema is documented and any future schema changes build on a known baseline.

**Why this priority**: This is important for documentation and traceability but the schema is already functional via JPA auto-DDL.

**Independent Test**: The initial migration script contains CREATE TABLE statements for both `users` and `devices` tables with column definitions matching the current JPA entity fields.

**Acceptance Scenarios**:

1. **Given** the source code repository, **When** a developer opens `src/main/resources/db/migration/V1__init_schema.sql`, **Then** it contains CREATE TABLE statements for `users` and `devices` with all existing columns, types, constraints, and indexes.
2. **Given** the migration script, **When** it is executed against a database, **Then** the resulting `users` and `devices` table schemas match the JPA entity definitions in `User.java` and `Device.java`.

### Edge Cases

- What happens if the Flyway migration runs and the `users` table already exists (e.g., from JPA auto-DDL in development)? The migration should handle this gracefully or Flyway should be configured to fail-fast for safety.
- What happens when a new column (role) is added through a migration, but existing code expects the old schema? The code must be updated to reference the new column before or at the same time.
- What happens to users who registered before the role column was introduced? They should receive a default value (CUSTOMER_USER) via a data migration or the column default.

## Requirements

### Functional Requirements

- **FR-001**: The system MUST use Flyway as the database migration tool, with all schema changes managed through versioned migration scripts placed in `src/main/resources/db/migration/`.
- **FR-002**: The initial migration script MUST create the `users` table with columns matching the existing `User.java` JPA entity definition, plus a `role` column with default value `CUSTOMER_USER`.
- **FR-003**: The initial migration script MUST create the `devices` table with columns matching the existing `Device.java` JPA entity definition.
- **FR-004**: When a new user registers via email or phone, the system MUST automatically store `CUSTOMER_USER` as the user's role.
- **FR-005**: When a new user registers via WeChat OAuth (scan or redirect), the system MUST automatically store `CUSTOMER_USER` as the user's role.
- **FR-006**: The User model MUST include a `role` field to persist the assigned role value.
- **FR-007**: The Flyway configuration MUST be compatible with PostgreSQL.

### Key Entities

- **User**: Represents a registered user account. Currently stores email, phone, username, password hash, token version, WeChat profile fields, and timestamps. Extended to include a `role` column.
- **Device**: Represents a device session associated with a user. Stores device ID, device type (web/ios/android), name, and timestamps.

## Success Criteria

### Measurable Outcomes

- **SC-001**: A developer can start the application against a fresh PostgreSQL database and have all tables created automatically by Flyway without manual SQL execution.
- **SC-002**: Every newly registered user (email, phone, or WeChat) has role `CUSTOMER_USER` stored in their user record.
- **SC-003**: All schema changes (both existing and future) are tracked through Git-versioned Flyway migration scripts.
- **SC-004**: The `flyway_schema_history` table is present and correctly records the initial migration after first startup.

## Assumptions

- Flyway will be configured via Spring Boot auto-configuration with standard `spring.flyway.*` properties.
- The existing JPA entities (`User.java`, `Device.java`) accurately represent the desired database schema.
- JPA `ddl-auto` will be set to `validate` or `none` after Flyway is introduced, to avoid conflicts between auto-DDL and migration scripts.
- The role value `CUSTOMER_USER` is a simple string column — no separate roles table or full RBAC system is needed at this stage.
- PostgreSQL is the target database; migration SQL will use PostgreSQL-compatible syntax.
