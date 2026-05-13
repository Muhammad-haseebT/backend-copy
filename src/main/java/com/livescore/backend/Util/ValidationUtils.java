package com.livescore.backend.Util;

import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Utility class for common validation operations across services.
 * Provides reusable validation methods to reduce code duplication and improve maintainability.
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // Prevent instantiation
    }

    /**
     * Validates that a string is not null and not blank.
     *
     * @param value     The string to validate
     * @param fieldName The name of the field for error messages
     * @return ResponseEntity with error if invalid, null if valid
     */
    public static ResponseEntity<?> validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", fieldName + " is required")
            );
        }
        return null;
    }

    /**
     * Validates that a Long ID is not null.
     *
     * @param id        The ID to validate
     * @param fieldName The name of the field for error messages
     * @return ResponseEntity with error if invalid, null if valid
     */
    public static ResponseEntity<?> validateRequiredId(Long id, String fieldName) {
        if (id == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", fieldName + " is required")
            );
        }
        return null;
    }

    /**
     * Validates that an object is not null.
     *
     * @param obj       The object to validate
     * @param fieldName The name of the field for error messages
     * @return ResponseEntity with error if invalid, null if valid
     */
    public static ResponseEntity<?> validateNotNull(Object obj, String fieldName) {
        if (obj == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", fieldName + " is required")
            );
        }
        return null;
    }

    /**
     * Creates a standardized bad request response with an error message.
     *
     * @param message The error message
     * @return ResponseEntity with error
     */
    public static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(
                Map.of("error", message)
        );
    }

    /**
     * Creates a standardized success response with a message.
     *
     * @param message The success message
     * @return ResponseEntity with success message
     */
    public static ResponseEntity<?> success(String message) {
        return ResponseEntity.ok().body(
                Map.of("message", message)
        );
    }

    /**
     * Creates a standardized success response with data.
     *
     * @param data The data to include in the response
     * @return ResponseEntity with data
     */
    public static ResponseEntity<?> successWithData(Map<String, Object> data) {
        return ResponseEntity.ok(data);
    }
}
