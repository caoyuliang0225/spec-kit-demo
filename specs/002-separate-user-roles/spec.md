# Feature Specification: Separate User Role Into Dedicated Table

**Feature Branch**: `002-separate-user-roles`
**Created**: 2026-05-12
**Status**: Draft
**Input**: "当新用户注册时，为用户添加了一个role，现在我要把用户和role单独存在一张表里，为后续功能做铺垫。"

## User Scenarios & Testing

### User Story 1 - Existing users retain their role after schema change (Priority: P1)

As a registered user of the platform, I expect my existing role assignment to be preserved when the system migrates role data from a user column to a dedicated table, so that my access permissions remain unchanged.

**Why this priority**: Data integrity is critical — no existing user should lose their role during migration. This protects against regressions before adding new features.

**Independent Test**: A user who was registered before the migration still has role `CUSTOMER_USER` after the schema change is applied.

**Acceptance Scenarios**:

1. **Given** a user was registered before the role table migration, **When** the migration runs, **Then** their role `CUSTOMER_USER` is preserved in the new table structure.
2. **Given** the migration has completed, **When** any existing user data is queried, **Then** each user has exactly one role record matching their original role.

---

### User Story 2 - New registrations receive default role from table structure (Priority: P1)

As a new user registering on the platform, I am automatically assigned the CUSTOMER_USER role, and that role is stored in the new dedicated role table, so that the system is ready for future role management features.

**Why this priority**: This is the core requirement — the role must come from the new table structure, not from a user column.

**Independent Test**: A new user completes registration, and their role is stored as a record in the role-related table with value `CUSTOMER_USER`.

**Acceptance Scenarios**:

1. **Given** a new user registers via email or phone, **When** the account is created, **Then** a role record with `CUSTOMER_USER` is created and linked to the user.
2. **Given** a new user registers via WeChat OAuth, **When** the account is created, **Then** a role record with `CUSTOMER_USER` is created and linked to the user.

---

### User Story 3 - Role table supports future role features (Priority: P2)

As a developer preparing for future features, I want the role data model to support multiple roles per user and role metadata, so that features like admin role assignment, role-based access control, and role management can be added without further schema changes.

**Why this priority**: The user explicitly requested this change "为后续功能做铺垫" (to pave the way for subsequent features). The data model should be forward-looking.

**Independent Test**: The role table design allows a user to be linked to multiple role records, and includes columns for role metadata (name, description).

**Acceptance Scenarios**:

1. **Given** the new role schema, **When** reviewed, **Then** it supports a many-to-many relationship between users and roles.
2. **Given** the new role schema, **When** reviewed, **Then** role definitions include a unique name and optional description.

### Edge Cases

- What happens if a user has no role record after migration? The migration script should create a default CUSTOMER_USER role for any user without one.
- What happens to the old `role` column on the `users` table? It should be removed or deprecated after migration is verified — the role data now lives in the dedicated table.
- What happens during the transition (data migration phase)? The system should handle both old and new structures briefly, or perform the migration atomically.

## Requirements

### Functional Requirements

- **FR-001**: The system MUST store user role assignments in a dedicated table, separate from the `users` table.
- **FR-002**: The role table MUST support multiple roles per user to allow future extensibility.
- **FR-003**: A roles reference table MUST define available role names (e.g., `CUSTOMER_USER`).
- **FR-004**: When a new user registers, the system MUST create a role assignment record linking the user to the `CUSTOMER_USER` role.
- **FR-005**: When a user registers via WeChat, the system MUST create the same role assignment record.
- **FR-006**: The existing `role` column on the `users` table MUST be migrated to the new table structure without data loss.
- **FR-007**: After migration, the `role` column on `users` MUST be removed to avoid dual-source-of-truth.
- **FR-008**: The migration MUST be implemented as a Flyway migration script.

### Key Entities

- **User**: Represents a registered user account. The `role` column will be removed after migration.
- **Role**: Represents a role definition. Contains a unique name (e.g., `CUSTOMER_USER`) and an optional description.
- **UserRole (junction)**: Links users to roles. Stores `user_id`, `role_id`, and optionally `assigned_at` timestamp. Supports many-to-many relationship.

## Success Criteria

### Measurable Outcomes

- **SC-001**: All existing users have their role preserved in the new table after migration, verified by comparing pre- and post-migration role data.
- **SC-002**: Every new user registration creates exactly one role record linked to the user with value `CUSTOMER_USER`.
- **SC-003**: The role table structure supports multiple roles per user without requiring future schema changes.
- **SC-004**: The `role` column on `users` is successfully removed after the migration without data loss or application errors.

## Assumptions

- The existing `role` column currently stores a single string value per user (e.g., `CUSTOMER_USER`).
- A normalized approach is preferred: a `roles` table for role definitions and a `user_roles` junction table for assignments.
- The initial seed data for roles includes only `CUSTOMER_USER` — additional roles can be added in future features.
- The migration will be a two-step Flyway script: first create tables and migrate data, then drop the old column.
- No existing tests are expected to break since the role behavior (default CUSTOMER_USER) remains unchanged.
