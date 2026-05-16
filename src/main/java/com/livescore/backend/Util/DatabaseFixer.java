package com.livescore.backend.Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseFixer {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:6969/test", "postgres", "Malik@786");
            Statement stmt = conn.createStatement();
            int deleted = stmt.executeUpdate("DELETE FROM cricket_ball WHERE batsman_id IS NULL");
            System.out.println("Deleted " + deleted + " corrupted balls.");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
