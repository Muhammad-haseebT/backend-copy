package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class TeamRequestDTO {
    private Long id;
    private Long teamId;
    private Long playerId;
    private String status;
}
