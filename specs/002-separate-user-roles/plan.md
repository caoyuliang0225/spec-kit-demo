# Implementation Plan: Separate User Role Into Dedicated Table

**Branch**: `002-separate-user-roles` | **Date**: 2026-05-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/002-separate-user-roles/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Normalize the user role from a column on the `users` table into a dedicated schema with `roles` and `user_roles` tables. Create a Flyway migration (V2) that creates the new tables, migrates existing role data, and drops the old `role` column. Update `AuthService.register()` and `wechatLogin()` to write role assignments to the junction table instead of the column. Remove the `role` field from `User.java` and add JPA entities for `Role` and `UserRole`.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.2.4, Flyway, PostgreSQL Driver, jjwt 0.12.5
**Storage**: PostgreSQL (existing), Redis (existing)
**Testing**: JUnit 5 + Mockito
**Target Platform**: Linux (Docker on Kubernetes)
**Project Type**: web-service (Spring Boot REST API)
**Performance Goals**: N/A — schema migration has no user-facing performance impact
**Constraints**: Must preserve data for all existing users (no loss); registration behavior must remain unchanged; the old `role` column must be removed after migration
**Scale/Scope**: Single service — Flyway migration + entity refactor + service update

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Rationale |
|-----------|--------|-----------|
| I. Layered Architecture | ✅ PASS | New Role/UserRole entities at model layer; AuthService updates at service layer. No layer violation. |
| II. Code Quality Compliance (NON-NEGOTIABLE) | ✅ PASS | New entities, migration script, and service changes must pass Alibaba + SonarQube scans. |
| III. Test Coverage (NON-NEGOTIABLE) | ✅ PASS | AuthService.register() and wechatLogin() changes need test updates. Existing coverage maintained. |
| IV. Container-Native Deployment | ✅ PASS | Flyway migration runs at startup within the existing container — no deployment change needed. |
| V. API-First Design | ✅ PASS | Role is assigned server-side; no API contract change. |

**Result**: GATE PASSED — no violations requiring Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/002-separate-user-roles/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output — Role and UserRole entities
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output — no new API contracts
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/portal/auth/
├── model/
│   ├── User.java                   # [MODIFY] Remove role field; add @ManyToMany relationship
│   ├── Role.java                   # [CREATE] Role entity
│   └── UserRole.java               # [CREATE] Junction entity (or use @ManyToMany mapping)
├── repository/
│   ├── RoleRepository.java         # [CREATE] JPA repository for Role
│   └── UserRoleRepository.java     # [CREATE] JPA repository for UserRole
├── service/
│   └── AuthService.java            # [MODIFY] Use UserRoleRepository instead of setRole()

src/main/resources/
└── db/migration/
    ├── V1__init_schema.sql          # [EXISTING] Unchanged
    └── V2__separate_user_roles.sql  # [CREATE] Create roles + user_roles, migrate data, drop role column

src/test/java/com/portal/auth/
├── AuthControllerTest.java         # [MODIFY] Remove role-related assertions
├── AuthServiceTest.java            # [MODIFY] Update role assignment test for junction table
└── RoleMigrationTest.java          # [CREATE] Verify V2 migration data integrity
```

**Structure Decision**: Standard Spring Boot Maven layout. New JPA entities follow existing patterns in the model package. Flyway V2 handles the migration in a single atomic script.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations. This section is intentionally left blank.
