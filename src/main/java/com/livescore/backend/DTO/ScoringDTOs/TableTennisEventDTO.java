package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;

@Data
public class TableTennisEventDTO {
    private Long    id;
    private String  eventType;
    private Integer gameNumber;
    private Integer eventTimeSeconds;
    private String  scoreSnapshot;
    private Long    playerId;
    private String  playerName;
    private Long    teamId;
    private String  teamName;
}
