---

description: "Task list for separating user role into dedicated table"
---

# Tasks: Separate User Role Into Dedicated Table

**Input**: Design documents from `specs/002-separate-user-roles/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), data-model.md, research.md

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

**Purpose**: No additional setup required — Flyway and project config already in place

- [x] T000 Flyway already configured from previous feature — no setup tasks needed

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: New entities, repositories, and V2 migration script that MUST exist before role data can be migrated or assigned

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T001 Create Flyway V2 migration at `src/main/resources/db/migration/V2__separate_user_roles.sql`:
      CREATE TABLE roles, INSERT CUSTOMER_USER seed, CREATE TABLE user_roles,
      migrate existing data from `users.role` column, ALTER TABLE users DROP COLUMN role
- [ ] T002 [P] Create `src/main/java/com/portal/auth/model/Role.java`:
      JPA entity with `id` (UUID), `name` (unique), `description` (nullable)
- [ ] T003 [P] Create `src/main/java/com/portal/auth/model/UserRole.java`:
      JPA entity with `id` (UUID), `user` (ManyToOne), `role` (ManyToOne), `assignedAt`
- [ ] T004 [P] Create `src/main/java/com/portal/auth/repository/RoleRepository.java`:
      JPA repository with `Optional<Role> findByName(String name)`
- [ ] T005 [P] Create `src/main/java/com/portal/auth/repository/UserRoleRepository.java`:
      JPA repository with standard CRUD
- [ ] T006 Modify `src/main/java/com/portal/auth/model/User.java`:
      Remove `role` field and getter/setter; add `@OneToMany List<UserRole> userRoles`

**Checkpoint**: Foundation ready — role entities and migration exist

---

## Phase 3: User Story 2 - Default Role via Junction Table (Priority: P1)

**Goal**: New user registrations create a role assignment in the `user_roles` table instead of setting the old `role` column

**Independent Test**: A new user registers, and a record in `user_roles` links them to `CUSTOMER_USER`

### Tests for User Story 2 ⚠️

- [ ] T007 [P] [US2] Update `src/test/java/com/portal/auth/AuthServiceTest.java`:
      Mock RoleRepository and UserRoleRepository; verify register() creates a UserRole record
- [ ] T008 [P] [US2] Create `src/test/java/com/portal/auth/RoleMigrationTest.java`:
      Unit test verifying V2 migration script contains CREATE TABLE, INSERT, and ALTER statements

### Implementation for User Story 2

- [ ] T009 [US2] Modify `AuthService.register()` in `src/main/java/com/portal/auth/service/AuthService.java`:
      Replace `user.setRole("CUSTOMER_USER")` with RoleRepository lookup +
      UserRoleRepository save
- [ ] T010 [US2] Modify `AuthService.wechatLogin()` in `src/main/java/com/portal/auth/service/AuthService.java`:
      Same replacement as T009 for the WeChat auto-registration path

**Checkpoint**: User Story 2 fully functional — new users get CUSTOMER_USER via junction table

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Ensure code quality compliance and CI readiness

- [ ] T011 Run `mvn verify` to ensure tests pass and no Alibaba/SonarQube violations are introduced

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — already complete
- **Foundational (Phase 2)**: No dependencies — blocks all user stories
- **User Story 2 (Phase 3)**: Depends on Foundational phase completion (entities + repositories must exist)
- **Polish (Phase 4)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 2 (P1)**: Can start after Foundational (Phase 2) — no other story dependencies

### Within Each User Story

- Tests written and FAIL before implementation
- Entities before repositories
- Repositories before service changes
- Story complete before moving to Polish

### Parallel Opportunities

- T002, T003, T004, T005 can all run in parallel (different files, no dependencies)
- T006 depends on T002 and T003 (User.java needs Role/UserRole to exist)
- T007 and T008 can run in parallel (test files)
- T009 and T010 depend on T006 (User.java modification must be complete)

---

## Parallel Example: Phase 3

```bash
# Launch all entity/repo tasks together:
Task: "Create Role.java in src/main/java/com/portal/auth/model/"
Task: "Create UserRole.java in src/main/java/com/portal/auth/model/"
Task: "Create RoleRepository.java in src/main/java/com/portal/auth/repository/"
Task: "Create UserRoleRepository.java in src/main/java/com/portal/auth/repository/"

# Then User.java modification (depends on Role + UserRole existing):
Task: "Modify User.java in src/main/java/com/portal/auth/model/"

# Then service changes + tests in parallel:
Task: "Modify AuthService.register()"
Task: "Modify AuthService.wechatLogin()"
Task: "Update AuthServiceTest.java"
Task: "Create RoleMigrationTest.java"
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 2: Foundational (entities + migration)
2. Complete Phase 3: User Story 2 (role assignment via junction table)
3. **STOP and VALIDATE**: Test registration with new role table
4. Deploy/demo if ready

### Incremental Delivery

1. Foundational done → Entities + migration ready
2. User Story 2 done → Role works via junction table (MVP!)
3. No additional user stories remaining in this batch

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
