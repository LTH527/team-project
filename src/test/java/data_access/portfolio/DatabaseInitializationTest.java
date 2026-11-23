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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInitializationTest {
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
    void initializeDatabaseCreatesAllTables() throws SQLException {
        DatabaseConnection.initializeDatabase();

        Set<String> tableNames = new HashSet<>();
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'table'")) {
            while (resultSet.next()) {
                tableNames.add(resultSet.getString("name"));
            }
        }

        assertTrue(tableNames.contains("portfolios"));
        assertTrue(tableNames.contains("holdings"));
        assertTrue(tableNames.contains("transactions"));
    }
}
