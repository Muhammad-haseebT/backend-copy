package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;

@Data
public class FutsalEventDTO {

    private Long    id;           // Used for media attachment (like ballId in cricket)
    private String  eventType;    // GOAL, OWN_GOAL, FOUL, YELLOW_CARD, RED_CARD, etc.
    private String  goalType;     // NORMAL, PENALTY, FREE_KICK, OWN_GOAL
    private String  cardType;     // null, YELLOW, RED (for FOUL events)
    private Integer half;
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