package com.actest.admin.repository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import com.actest.admin.config.Config;

public class DatabaseConnection {
    private static final String URL = Config.get("DB_URL", "jdbc:sqlite:actest_admin.db");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            String sql;
            try (InputStream inputStream = DatabaseConnection.class.getResourceAsStream("/schema.sql")) {
                if (inputStream == null) {
                    System.err.println("schema.sql not found!");
                    return;
                }
                sql = new BufferedReader(new InputStreamReader(inputStream))
                        .lines().collect(Collectors.joining("\n"));
            }

            for (String statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement);
                }
            }

            // Simple migration: add missing columns if they don't exist
            try {
                stmt.execute("ALTER TABLE exams ADD COLUMN creator_id INTEGER");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("ALTER TABLE questions ADD COLUMN exam_id INTEGER");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("ALTER TABLE questions ADD COLUMN options TEXT");
            } catch (SQLException ignored) {
            }
            try {
                stmt.execute("ALTER TABLE questions ADD COLUMN correct_option_index INTEGER");
            } catch (SQLException ignored) {
            }

            System.out.println("Local database initialized and migrated successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
