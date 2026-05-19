package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;

@Data
public class HockeyEventDTO {

    private Long    id;
    private String  eventType;    // GOAL, OWN_GOAL, FOUL, GREEN_CARD, YELLOW_CARD, RED_CARD, etc.
    private String  goalType;     // NORMAL, FIELD_GOAL, PENALTY_CORNER, PENALTY_STROKE
    private String  cardType;     // null, GREEN, YELLOW, RED (for FOUL events)
    private Integer period;
    private Integer eventTimeSeconds;
    private Boolean extraTime;

    // Scorer / primary player
    private Long   scorerId;
    private String scorerName;

    // Assist (goals only)
    private Long   assistPlayerId;
    private String assistPlayerName;

    // Team
    private Long   teamId;
    private String teamName;

    // Substitution: player coming in
    private Long   inPlayerId;
    private String inPlayerName;
}
