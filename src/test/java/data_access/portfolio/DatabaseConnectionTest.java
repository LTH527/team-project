package data_access.portfolio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseConnectionTest {
    private Path databaseFile;

    @BeforeEach
    void setUp() throws IOException {
        databaseFile = Files.createTempFile("portfolio-test-", ".db");
        DatabaseConnection.setDatabaseUrl(
                "jdbc:sqlite:" + databaseFile.toAbsolutePath());
    }

    @AfterEach
    void tearDown() throws IOException {
        DatabaseConnection.resetDatabaseUrl();
        Files.deleteIfExists(databaseFile);
    }

    @Test
    public void connectsToSQLite() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertEquals(1, resultSet.getInt(1));
        }
    }

    @Test
    public void foreignKeysAreEnabledForEveryConnection() throws SQLException {
        assertForeignKeysEnabled();
        assertForeignKeysEnabled();
    }

    private void assertForeignKeysEnabled() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA foreign_keys")) {
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }
    }
}
