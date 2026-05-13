package com.livescore.backend.DTO.ScoringDTOs;

import com.livescore.backend.DTO.PlayerSimpleDTO;
import lombok.Data;
import java.util.List;

@Data
public class FutsalScoreDTO {

    // ── Request fields (frontend → backend) ────────────────────────
    private Long    matchId;
    private Long    teamId;         // Team who did the event
    private Long    playerId;       // Scorer / fouler / player going OUT
    private Long    inPlayerId;     // Sub: player coming IN
    private Long    assistPlayerId; // Goal assist player (optional)
    private Long    outPlayerId;    // Alias for playerId in sub context

    // GOAL, OWN_GOAL, FOUL, YELLOW_CARD, RED_CARD,
    // SUBSTITUTION, END_HALF, EXTRA_TIME, TIMEOUT
    private String  eventType;

    // NORMAL, PENALTY, FREE_KICK, OWN_GOAL
    private String  goalType;

    // For foul: null = simple foul, YELLOW, RED
    private String  cardType;

    // True if this event is in extra time
    private boolean extraTime;

    // Undo flag
    private boolean undo;

    // ── Response fields (backend → frontend) ────────────────────────
    private Integer team1Score;
    private Integer team2Score;
    private Integer team1Fouls;     // Resets after 5 in futsal
    private Integer team2Fouls;
    private Integer team1YellowCards;
    private Integer team2YellowCards;
    private Integer team1RedCards;
    private Integer team2RedCards;
    private Integer currentHalf;   // 1, 2, or 3 (extra time)
    private String  status;        // LIVE, HALF_TIME, EXTRA_TIME, COMPLETED
    private boolean inExtraTime;

    // Timer — frontend syncs from server time
    private Long    halfStartTime;       // epoch ms
    private Integer halfDurationMinutes; // 25 for regular, 5 for extra time

    // Full events list — for Events tab + media attachment
    private List<FutsalEventDTO> futsalEvents;

    // Winner name / UNDO / error messages
    private String  comment;
    private List<PlayerSimpleDTO> team1Players;   // full approved squad
    private List<PlayerSimpleDTO> team2Players;
    private List<PlayerSimpleDTO> team1OnField;   // currently on field
    private List<PlayerSimpleDTO> team2OnField;
}