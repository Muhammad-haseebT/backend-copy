package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;

@Data
public class ChessEventDTO {
    private Long    id;
    private String  eventType;
    private String  moveNotation;
    private Integer moveNumber;
    private Integer eventTimeSeconds;
    private Long    playerId;
    private String  playerName;
    private Long    teamId;
    private String  teamName;
}
