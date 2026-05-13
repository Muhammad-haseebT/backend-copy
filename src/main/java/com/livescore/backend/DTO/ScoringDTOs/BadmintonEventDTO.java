package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;

@Data
public class BadmintonEventDTO {
    private Long    id;
    private String  eventType;
    private Integer gameNumber;
    private Integer eventTimeSeconds;
    private String  scoreSnapshot;

    private Long   playerId;
    private String playerName;

    private Long   inPlayerId;
    private String inPlayerName;

    private Long   teamId;
    private String teamName;
}
