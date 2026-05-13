package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;

@Data
public class VolleyballEventDTO {
    private Long    id;
    private String  eventType;
    private Integer setNumber;
    private Integer eventTimeSeconds;

    private Long   playerId;
    private String playerName;

    private Long   inPlayerId;
    private String inPlayerName;

    private Long   teamId;
    private String teamName;
}
