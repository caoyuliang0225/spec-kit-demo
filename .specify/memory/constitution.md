<!--
  Sync Impact Report
  ==================
  Version change: N/A (template) → 1.0.0
  Modified principles: N/A (all new)
  Added sections:
    - Principle I: Layered Architecture & API Design
    - Principle II: Code Quality Compliance (NON-NEGOTIABLE)
    - Principle III: Test Coverage (NON-NEGOTIABLE)
    - Principle IV: Container-Native Deployment
    - Principle V: API-First Design
    - Technology Stack & Compliance Standards
    - Quality Gates & Development Workflow
  Removed sections: N/A
  Templates requiring updates:
    - .specify/templates/plan-template.md ✅ (no changes needed - Constitution Check gate aligns)
    - .specify/templates/spec-template.md ✅ (no changes needed)
    - .specify/templates/tasks-template.md ✅ (no changes needed)
    - README.md ✅ (no changes needed - already reflects architecture)
  Follow-up TODOs: none
-->

# portal-auth Constitution

## Core Principles

### I. Layered Architecture & API Design
The original layered architecture MUST be preserved:
Controller → Service → Repository, with DTOs for data transfer.
Each layer has a single responsibility and communicates only with
the adjacent layer. Controllers handle HTTP concerns, services
contain business logic, repositories manage data access.

### II. Code Quality Compliance (NON-NEGOTIABLE)
All source code MUST pass both Alibaba Java Coding Guidelines
scan AND SonarQube quality gate with zero P1/P2 violations and
zero blocker/critical issues. Any violation MUST be fixed before
merge. No exceptions without documented governance override.

### III. Test Coverage (NON-NEGOTIABLE)
Every controller (API) and service class MUST have >= 90% unit
test line coverage, verified by JaCoCo or equivalent tool.
Tests MUST cover both happy paths and error/edge cases.
Coverage gates MUST be enforced in CI — a build failing coverage
threshold SHALL NOT be merged.

### IV. Container-Native Deployment
The service MUST be packaged as a Docker image and deployed on
Kubernetes (k8s). All configuration MUST be externalized via
environment variables or ConfigMaps. The Docker image MUST be
reproducible and versioned with the application version.

### V. API-First Design
All public interfaces MUST follow RESTful conventions with
consistent request/response schemas. Input validation MUST be
applied at the controller layer using Bean Validation
annotations. Error responses MUST follow a uniform structure.

## Technology Stack & Compliance Standards

| Component | Technology | Constraint |
|---|---|---|
| Language | Java 17 | MUST comply with Alibaba Coding Guidelines |
| Framework | Spring Boot 3.2.4 | Layered architecture enforced |
| Database | PostgreSQL | JPA repository layer |
| Cache | Redis | Token/OTP storage |
| Auth | JWT (jjwt 0.12.5) + BCrypt | Stateless auth |
| Build | Maven | SonarQube + JaCoCo plugins |
| Quality | Alibaba Guidelines | Zero P1/P2 violations |
| Quality | SonarQube | Zero blocker/critical issues |
| Test | JUnit 5 + Mockito | >= 90% controller + service coverage |
| Container | Docker | Multi-stage build, < 200MB |
| Orchestration | Kubernetes | Deployable via YAML manifests |

## Quality Gates & Development Workflow

1. **Pre-commit**: Run `mvn verify` — must pass tests and
   Alibaba Java Coding Guidelines checks.
2. **Pre-merge**: SonarQube quality gate must pass with zero
   new issues.
3. **Coverage gate**: Controller + service coverage >= 90%,
   measured by JaCoCo and enforced at CI level.
4. **Review gate**: Every PR MUST include at least one reviewer;
   code quality scan results MUST be attached.
5. **Deployment gate**: Only images built from passing CI
   pipelines SHALL be deployed to k8s.

## Governance

This constitution defines non-negotiable principles for the
portal-auth service. Amendments require a documented proposal,
team review, and an explicit version bump.

**Amendment procedure**: Propose changes via PR to this file.
The PR must include a Sync Impact Report. Approval requires
majority consent from active contributors.

**Versioning policy**:
- MAJOR: Backward-incompatible principle removal/redefinition.
- MINOR: New principle or materially expanded guidance.
- PATCH: Clarifications, typo fixes, non-semantic refinements.

**Compliance review**: Every implementation plan MUST include a
Constitution Check section verifying alignment. Violations MUST
be documented in Complexity Tracking.

**Version**: 1.0.0 | **Ratified**: 2026-05-12 | **Last Amended**: 2026-05-12
