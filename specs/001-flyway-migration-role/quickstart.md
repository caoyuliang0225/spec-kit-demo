# Quickstart: Flyway Migration & Default Role

## Prerequisites

- Java 17, PostgreSQL, Redis (standard project setup)
- No prior database schema required — Flyway creates tables on first startup

## Setup

```bash
# 1. Add Flyway dependency to pom.xml
# See data-model.md for exact dependency coordinates

# 2. Configure application.yml
# spring.jpa.hibernate.ddl-auto=validate
# spring.flyway.enabled=true
# spring.flyway.locations=classpath:db/migration

# 3. Create migration script
# Place V1__init_schema.sql in src/main/resources/db/migration/
# See data-model.md for the full SQL

# 4. Update User.java model — add role field
# private String role = "CUSTOMER_USER";

# 5. Update AuthService.java — set role on register/wechatLogin
# user.setRole("CUSTOMER_USER");

# 6. Run tests
./mvnw test
```

## Verification

1. Start the application with a fresh database
2. Check that `users` and `devices` tables are created automatically
3. Check that `flyway_schema_history` table exists
4. Register a new user — their role should be `CUSTOMER_USER`
