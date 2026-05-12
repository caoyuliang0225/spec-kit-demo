# Implementation Plan: Flyway Database Migration & Default User Role

**Branch**: `001-flyway-migration-role` | **Date**: 2026-05-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `specs/001-flyway-migration-role/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Introduce Flyway for version-controlled database migrations, replacing JPA auto-DDL. Generate the initial migration script (`V1__init_schema.sql`) that creates the `users` and `devices` tables from existing JPA entity definitions, plus a `role` column on `users` with default value `CUSTOMER_USER`. When a user registers (email/phone or WeChat), the service layer automatically assigns `CUSTOMER_USER` as the default role.

## Technical Context

**Language/Version**: Java 17
**Primary Dependencies**: Spring Boot 3.2.4, Flyway (new), PostgreSQL Driver, jjwt 0.12.5
**Storage**: PostgreSQL (existing), Redis (existing)
**Testing**: JUnit 5 + Mockito
**Target Platform**: Linux (Docker on Kubernetes)
**Project Type**: web-service (Spring Boot REST API)
**Performance Goals**: N/A — infrastructure feature, no user-facing performance impact
**Constraints**: Must preserve layered architecture; Flyway scripts must pass Alibaba Java Coding Guidelines checks; must keep PostgreSQL compatibility
**Scale/Scope**: Single service — Flyway runs embedded in the application at startup

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Rationale |
|-----------|--------|-----------|
| I. Layered Architecture | ✅ PASS | Flyway integration at data layer; role assignment in service layer (AuthService). No layer violation. |
| II. Code Quality Compliance (NON-NEGOTIABLE) | ✅ PASS | New migration scripts and Java changes must pass Alibaba + SonarQube scans. Standard code addition. |
| III. Test Coverage (NON-NEGOTIABLE) | ✅ PASS | AuthService.register() and wechatLogin() changes need tests verifying default role assignment. New tests will be added. |
| IV. Container-Native Deployment | ✅ PASS | Flyway runs at startup within the existing Spring Boot container — no deployment change needed. |
| V. API-First Design | ✅ PASS | Role is assigned server-side; no API contract change. |

**Result**: GATE PASSED — no violations requiring Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/001-flyway-migration-role/
├── plan.md              # This file
├── research.md          # Phase 0 output — resolved unknowns below
├── data-model.md        # Phase 1 output — extended User entity
├── quickstart.md        # Phase 1 output — setup instructions
├── contracts/           # Phase 1 output — no new API contracts needed
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/portal/auth/
├── model/
│   └── User.java                  # [MODIFY] Add role field
├── service/
│   └── AuthService.java           # [MODIFY] Set default role on register/wechatLogin

src/main/resources/
├── application.yml                # [MODIFY] Add Flyway config, disable ddl-auto
└── db/migration/
    └── V1__init_schema.sql         # [CREATE] Initial migration script

src/test/java/com/portal/auth/
├── AuthControllerTest.java        # [MODIFY] Add role assertion to registration test
└── AuthServiceTest.java           # [CREATE] Service-layer tests for role assignment
```

**Structure Decision**: Standard Spring Boot Maven layout preserved. Flyway migration scripts follow the `src/main/resources/db/migration/` convention. JPA `ddl-auto` changed from `update`/`create` to `validate` to prevent conflicts.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations. This section is intentionally left blank.
