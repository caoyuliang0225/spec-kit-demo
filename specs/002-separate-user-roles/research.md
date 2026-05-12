# Research: Separate User Role Table

## Normalization Strategy

**Decision**: Create `roles` lookup table + `user_roles` junction table.
**Rationale**: A normalized schema with a junction table supports future many-to-many
role assignments. Each user gets one CUSTOMER_USER role initially, but the structure
allows adding admin roles, multiple roles per user, and role metadata without schema
changes.

## Flyway Migration Strategy (V2)

**Decision**: Single atomic migration script (`V2__separate_user_roles.sql`).
**Steps**:
1. CREATE TABLE `roles` with `id`, `name` (UNIQUE), `description`.
2. INSERT seed role: `CUSTOMER_USER`.
3. CREATE TABLE `user_roles` with `user_id`, `role_id`, `assigned_at`.
4. INSERT into `user_roles` SELECT from existing `users.role` column, joining on role name.
5. ALTER TABLE `users` DROP COLUMN `role`.

JPA/`@PrePersist` and `@PreUpdate` on the junction entity set timestamps automatically.

## Legacy Data Fill Strategy

**Decision**: Users with NULL or missing role get CUSTOMER_USER during migration.
**Rationale**: The existing column has a DEFAULT 'CUSTOMER_USER', so no existing
user should have NULL. The migration handles NULL defensively with COALESCE.

## JPA Mapping

**Decision**: Use `@ManyToMany` on User → Role via user_roles join table, or
explicit UserRole entity if additional fields are needed on the join (e.g., timestamps).
**Rationale**: The `assigned_at` timestamp on the join suggests an explicit entity
is preferable for future extensibility (e.g., assignment source, expiry date).

## Old Column Removal Impact

**Decision**: Remove the `role` field from User.java after the V2 migration is verified.
**Rationale**: Once the data is migrated and verified, the field becomes dead code.
Removing it ensures no accidental dual-source-of-truth writes.
