package com.portal.auth;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleMigrationTest {

    @Test
    void v2MigrationScriptExists() {
        InputStream is = getClass().getResourceAsStream("/db/migration/V2__separate_user_roles.sql");
        assertNotNull(is, "V2__separate_user_roles.sql must exist in classpath:db/migration/");
    }

    @Test
    void v2CreatesRolesTable() {
        String content = readMigrationScript();
        assertTrue(content.contains("CREATE TABLE roles"),
                "V2 must create roles table");
    }

    @Test
    void v2CreatesUserRolesTable() {
        String content = readMigrationScript();
        assertTrue(content.contains("CREATE TABLE user_roles"),
                "V2 must create user_roles junction table");
    }

    @Test
    void v2SeedsCustomerUser() {
        String content = readMigrationScript();
        assertTrue(content.contains("CUSTOMER_USER"),
                "V2 must seed CUSTOMER_USER role");
    }

    @Test
    void v2MigratesExistingData() {
        String content = readMigrationScript();
        assertTrue(content.contains("INSERT INTO user_roles"),
                "V2 must migrate existing role data");
    }

    @Test
    void v2DropsOldColumn() {
        String content = readMigrationScript();
        assertTrue(content.contains("DROP COLUMN role"),
                "V2 must drop the old role column");
    }

    private String readMigrationScript() {
        try (InputStream is = getClass().getResourceAsStream("/db/migration/V2__separate_user_roles.sql")) {
            assertNotNull(is, "V2__separate_user_roles.sql must exist");
            return new String(is.readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read migration script", e);
        }
    }
}
