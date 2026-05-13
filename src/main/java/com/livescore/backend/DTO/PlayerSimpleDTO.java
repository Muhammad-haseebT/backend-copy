package com.livescore.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight player reference — used for available batters / bowlers
 * in cricket scoring DTO so frontend doesn't need a separate API call.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerSimpleDTO {
    private Long   id;
    private String name;
}