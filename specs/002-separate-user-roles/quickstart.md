# Quickstart: Separate User Role Table

## Prerequisites

- Previous feature (Flyway migration) must be completed
- V1__init_schema.sql must exist in `src/main/resources/db/migration/`

## Setup

```bash
# 1. Create Role.java entity
# Place at: src/main/java/com/portal/auth/model/Role.java

# 2. Create UserRole.java junction entity
# Place at: src/main/java/com/portal/auth/model/UserRole.java

# 3. Create RoleRepository.java
# Place at: src/main/java/com/portal/auth/repository/RoleRepository.java

# 4. Create UserRoleRepository.java
# Place at: src/main/java/com/portal/auth/repository/UserRoleRepository.java

# 5. Create V2 migration
# Place at: src/main/resources/db/migration/V2__separate_user_roles.sql

# 6. Modify User.java — remove role field, add relationship

# 7. Modify AuthService.java — use UserRoleRepository

# 8. Run tests
mvn test
```

## Verification

1. Start the application (Flyway runs V2 migration)
2. Check that `roles` and `user_roles` tables exist
3. Check that existing users have their role in `user_roles`
4. Register a new user — they get CUSTOMER_USER in `user_roles`
5. Verify `role` column is gone from `users` table
