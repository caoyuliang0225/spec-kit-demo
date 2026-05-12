package com.portal.auth;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlywayMigrationTest {

    @Test
    void migrationScriptExists() {
        InputStream is = getClass().getResourceAsStream("/db/migration/V1__init_schema.sql");
        assertNotNull(is, "V1__init_schema.sql must exist in classpath:db/migration/");
    }

    @Test
    void migrationScriptContainsUsersTable() {
        String content = readMigrationScript();
        assertTrue(content.contains("CREATE TABLE users"),
                "Migration script must define users table");
        assertTrue(content.contains("role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER_USER'"),
                "Migration script must include role column with CUSTOMER_USER default");
    }

    @Test
    void migrationScriptContainsDevicesTable() {
        String content = readMigrationScript();
        assertTrue(content.contains("CREATE TABLE devices"),
                "Migration script must define devices table");
        assertTrue(content.contains("FOREIGN KEY (user_id) REFERENCES users(id)"),
                "Migration script must include foreign key from devices to users");
    }

    private String readMigrationScript() {
        try (InputStream is = getClass().getResourceAsStream("/db/migration/V1__init_schema.sql")) {
            assertNotNull(is, "V1__init_schema.sql must exist");
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read migration script", e);
        }
    }
}
