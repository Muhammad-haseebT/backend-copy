package com.livescore.backend.DTO.ScoringDTOs;

import com.livescore.backend.DTO.PlayerSimpleDTO;
import lombok.Data;
import java.util.List;

@Data
public class HockeyScoreDTO {

    // ── Request fields (frontend → backend) ────────────────────────
    private Long    matchId;
    private Long    teamId;         // Team who did the event
    private Long    playerId;       // Scorer / fouler / player going OUT
    private Long    inPlayerId;     // Sub: player coming IN
    private Long    assistPlayerId; // Goal assist player (optional)
    private Long    outPlayerId;    // Alias for playerId in sub context

    // GOAL, OWN_GOAL, FOUL, GREEN_CARD, YELLOW_CARD, RED_CARD,
    // SUBSTITUTION, PENALTY_CORNER, PENALTY_STROKE, END_PERIOD, TIMEOUT
    private String  eventType;

    // NORMAL, FIELD_GOAL, PENALTY_CORNER, PENALTY_STROKE
    private String  goalType;

    // For foul: null = simple foul, GREEN, YELLOW, RED
    private String  cardType;

    // True if this event is in extra time
    private boolean extraTime;

    // Undo flag
    private boolean undo;

    // ── Response fields (backend → frontend) ────────────────────────
    private Integer team1Score;
    private Integer team2Score;
    private Integer team1Fouls;
    private Integer team2Fouls;
    private Integer team1YellowCards;
    private Integer team2YellowCards;
    private Integer team1RedCards;
    private Integer team2RedCards;
    private Integer team1GreenCards;
    private Integer team2GreenCards;
    private Integer team1PenaltyCorners;
    private Integer team2PenaltyCorners;
    private Integer currentPeriod;  // 1, 2, 3, 4(extra)
    private String  status;         // LIVE, BREAK, EXTRA_TIME, COMPLETED
    private boolean inExtraTime;

    // Timer — frontend syncs from server time
    private Long    periodStartTime;        // epoch ms
    private Integer periodDurationMinutes;  // 15 for regular, configurable

    // Full events list — for Events tab + media attachment
    private List<HockeyEventDTO> hockeyEvents;

    // Winner name / UNDO / error messages
    private String  comment;
    private List<PlayerSimpleDTO> team1Players;   // full approved squad
    private List<PlayerSimpleDTO> team2Players;
    private List<PlayerSimpleDTO> team1OnField;   // currently on field
    private List<PlayerSimpleDTO> team2OnField;
}
