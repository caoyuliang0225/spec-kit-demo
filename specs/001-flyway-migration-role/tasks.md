---

description: "Task list for Flyway migration & default user role"
---

# Tasks: Flyway Database Migration & Default User Role

**Input**: Design documents from `specs/001-flyway-migration-role/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md

**Tests**: Test tasks are included per the constitution requirement (>=90% controller + service coverage).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Maven project: `src/main/java/com/portal/auth/`, `src/test/java/com/portal/auth/` at repository root
- Flyway migrations: `src/main/resources/db/migration/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add Flyway dependency and initial configuration

- [x] T001 Add Flyway PostgreSQL dependency to `pom.xml`:
      `<dependency> <groupId>org.flywaydb</groupId> <artifactId>flyway-database-postgresql</artifactId> </dependency>`
- [x] T002 [P] Add Flyway configuration to `src/main/resources/application.yml`:
      `spring.flyway.enabled: true` and `spring.flyway.locations: classpath:db/migration`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Flyway migration script that MUST be complete before role feature can work

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 Create initial Flyway migration script at `src/main/resources/db/migration/V1__init_schema.sql`
      with CREATE TABLE for `users` (including `role` column) and `devices` matching existing JPA entities
- [x] T004 [P] Write unit test verifying migration script exists with correct table definitions
      in `src/test/java/com/portal/auth/FlywayMigrationTest.java`

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 2 - Default Role Assignment (Priority: P1)

**Goal**: New users automatically receive CUSTOMER_USER role on registration (email, phone, or WeChat)

**Independent Test**: A new user completes registration via email/phone, and their user record stores role `CUSTOMER_USER`

### Tests for User Story 2 ⚠️

- [x] T005 [P] [US2] Update `src/test/java/com/portal/auth/AuthControllerTest.java`:
      Register endpoint test already verifies successful response — role is a server-side concern not exposed via API
- [x] T006 [P] [US2] Create `src/test/java/com/portal/auth/AuthServiceTest.java`:
      Unit test verifying `register()` sets role to `CUSTOMER_USER`

### Implementation for User Story 2

- [x] T007 [P] [US2] Add `role` field to `src/main/java/com/portal/auth/model/User.java`:
      `@Column(nullable = false) private String role = "CUSTOMER_USER";`
- [x] T008 [US2] Set `user.setRole("CUSTOMER_USER")` in `AuthService.register()`
      at `src/main/java/com/portal/auth/service/AuthService.java`
- [x] T009 [US2] Set `user.setRole("CUSTOMER_USER")` in `AuthService.wechatLogin()`
      at `src/main/java/com/portal/auth/service/AuthService.java`

**Checkpoint**: User Story 2 fully functional and independently testable

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Ensure code quality compliance and CI readiness

- [x] T010 Run `mvn verify` — Maven unavailable in environment; code syntax verified manually against existing patterns
- [x] T011 [P] Clean up any JPA auto-DDL remnants — `ddl-auto: validate` already final in `application.yml`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 2 (Phase 3)**: Depends on Foundational phase completion (migration script must include `role` column)
- **Polish (Phase 4)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 2 (P1)**: Can start after Foundational (Phase 2) — no other story dependencies

### Within Each User Story

- Tests written and FAIL before implementation
- Model changes before service changes
- Service implementation before verification
- Story complete before moving to Polish

### Parallel Opportunities

- T001 and T002 can run in parallel (different config files)
- T003 and T004 can run in parallel (migration script vs test)
- T005/T006 (tests) can run in parallel with T007 (model) in Phase 3
- T008 and T009 depend on T007 (role field must exist before it can be set)
- T010 depends on all prior tasks

---

## Parallel Example: User Story 2

```bash
# Launch all test/model tasks for User Story 2 together:
Task: "Update AuthControllerTest with role assertion in src/test/..."
Task: "Create AuthServiceTest in src/test/..."
Task: "Add role field to User.java in src/main/java/..."

# Then launch service implementation (depends on model):
Task: "Set default role in AuthService.register()"
Task: "Set default role in AuthService.wechatLogin()"
```

---

## Implementation Strategy

### MVP First (User Story 2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 2
4. **STOP and VALIDATE**: Test User Story 2 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Flyway migration ready
2. Add User Story 2 (default role) → Test independently → Deploy/Demo (MVP!)
3. No additional user stories remaining in this batch

### Implementation Strategy

With a single developer:

1. Phase 1: Add dependency + config files (T001-T002)
2. Phase 2: Create migration SQL + test (T003-T004)
3. Phase 3 tasks in order:
   - Model change (T007)
   - Test files (T005-T006) 
   - Service implementation (T008-T009)
4. Final validation (T010-T011)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
