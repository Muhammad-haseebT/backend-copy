package com.livescore.backend.DTO;

import jakarta.persistence.Column;
import lombok.Data;

import java.io.File;

@Data
public class MediaDTO {
    private Long matchId;
    private Long ballId;
    private String comment; // ✅ NEW
}
