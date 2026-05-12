# Research: Flyway Integration & Role Field

## Flyway Dependency

**Decision**: Add `flyway-database-postgresql` to `pom.xml`
**Rationale**: Spring Boot 3.2.4 auto-configures Flyway when the dependency is on the classpath. The PostgreSQL-specific module is required for PostgreSQL compatibility.
**Details**:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```
**Alternatives considered**: Liquibase — Flyway is simpler for SQL-first migrations and is the default Spring Boot recommendation.

## JPA ddl-auto Strategy

**Decision**: Set `spring.jpa.hibernate.ddl-auto=validate` after Flyway is introduced
**Rationale**: Flyway manages the schema; Hibernate should only validate that the schema matches entities at startup. This prevents conflicts between auto-DDL and migration scripts.

## Migration Script V1: users table DDL

**Decision**: Create `V1__init_schema.sql` with PostgreSQL-compatible DDL
**Rationale**: The `users` table must match the `User.java` entity. UUID primary key uses PostgreSQL `UUID` type. Since `GenerationType.UUID` is used in the entity, Hibernate generates UUID strings at the application level — the column type should be `VARCHAR(36)` or `UUID` with Hibernate handling the mapping. Standard practice with PostgreSQL is to use the `uuid` type with `uuid-ossp` extension, but since the entity uses `@GeneratedValue(strategy = GenerationType.UUID)` which generates Java UUIDs, `VARCHAR(36)` is more portable and matches Hibernate's default UUID string mapping.

Actually, let me reconsider. With JPA `GenerationType.UUID` and PostgreSQL, Hibernate stores UUIDs as strings by default (character varying). The column type should be `VARCHAR(36)` to store the UUID string representation.

## Migration Script V1: devices table DDL

**Decision**: Create with `user_id` foreign key reference to `users(id)`
**Rationale**: The `Device` entity has a `userId` field that references `User.id`. This is enforced via a foreign key constraint.

## Default Role

**Decision**: Add `role` column with `NOT NULL DEFAULT 'CUSTOMER_USER'`
**Rationale**: Existing users without the column get the default value on migration. New registrations set the field server-side. The column default ensures backward compatibility.

## Flyway Configuration

**Decision**: Use Spring Boot defaults with PostgreSQL compatibility
**Rationale**:
- `spring.flyway.enabled=true` (default)
- `spring.flyway.locations=classpath:db/migration` (default)
- No baseline-on-migrate needed since this is the first migration against a fresh schema
