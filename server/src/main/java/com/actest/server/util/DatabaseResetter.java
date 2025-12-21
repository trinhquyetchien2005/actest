package com.actest.server.util;

import com.actest.server.repository.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseResetter {
    public static void main(String[] args) {
        System.out.println("Resetting Server Database...");
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            // Drop tables
            String dropSql = "DROP TABLE IF EXISTS friend_requests, messages, questions, exams, users CASCADE";
            stmt.execute(dropSql);
            System.out.println("Tables dropped.");

            // Re-initialize
            DatabaseConnection.initializeDatabase();
            System.out.println("Database re-initialized.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
