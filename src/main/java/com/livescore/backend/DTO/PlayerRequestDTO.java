package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class PlayerRequestDTO {
    private Long id;
    private Long playerId;
    private Long teamId;
    private Long tournamentId;
    private String status;
    private String us;

}

