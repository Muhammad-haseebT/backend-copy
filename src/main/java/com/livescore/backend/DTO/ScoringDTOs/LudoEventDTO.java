package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;

@Data
public class LudoEventDTO {
    private Long    id;
    private String  eventType;
    private Integer eventTimeSeconds;
    private Long    playerId;
    private String  playerName;
    private Long    teamId;
    private String  teamName;
}
