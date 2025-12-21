package com.actest.server.repository;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/actest";
    private static final String USER = "chien";
    private static final String PASSWORD = "totolink";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            InputStream inputStream = DatabaseConnection.class.getResourceAsStream("/schema.sql");
            if (inputStream == null) {
                System.err.println("schema.sql not found!");
                return;
            }

            String sql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                sql = reader.lines().collect(Collectors.joining("\n"));
            }

            for (String statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement);
                }
            }
            System.out.println("Database initialized successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
