package com.livescore.backend.Util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseFixer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixNotificationConstraint() {
        try {
            System.out.println("Attempting to fix notification_type_check constraint...");
            // Drop the old constraint
            jdbcTemplate.execute("ALTER TABLE notification DROP CONSTRAINT IF EXISTS notification_type_check");
            // Add the new one with FIXTURE included
            jdbcTemplate.execute("ALTER TABLE notification ADD CONSTRAINT notification_type_check CHECK (type IN ('MATCH_REMINDER', 'MATCH_START', 'FIXTURE'))");
            System.out.println("Constraint fixed successfully!");
        } catch (Exception e) {
            System.err.println("Failed to fix constraint: " + e.getMessage());
        }
    }
}
