package com.livescore.backend.DTO.ScoringDTOs;

import com.livescore.backend.DTO.CricketBallsScoringDTO;
import com.livescore.backend.DTO.PlayerSimpleDTO;
import com.livescore.backend.DTO.PlayerStatDTO;
import lombok.Data;

import java.util.List;
@Data
public class ScoreDTO {
    // ═══════════════════════════════════
    // ✅ EXISTING CRICKET FIELDS — DO NOT TOUCH
    // ═══════════════════════════════════
    private int runs;
    private int overs;
    private int wickets;
    private int balls;
    private String status;
    private int target;
    private String event;
    private Long teamId;
    private Long matchId;
    private Long batsmanId;
    private Long bowlerId;
    private Long fielderId;
    private int extra;
    private String extraType;
    private String eventType;
    private String dismissalType;
    private Boolean isLegal;
    private Long inningsId;
    private Long outPlayerId;
    private Long newPlayerId;
    private int runsOnThisBall;
    private int extrasThisBall;
    private boolean isFour;
    private boolean isSix;
    private boolean firstInnings = true;
    private boolean isSuperOver = false;

    private String comment;
    private Long mediaId;
    private Long nonStrikerId;
    private double crr;
    private double rrr;
    private String matchStatus;
    private boolean undo = false;
    private PlayerStatDTO batsman1Stats;
    private PlayerStatDTO batsman2Stats;
    private PlayerStatDTO bowlerStats;
    private List<CricketBallsScoringDTO> cricketBalls;
    private int dlsTarget;
    // ═══════════════════════════════════
    // 🆕 FUTSAL / FOOTBALL FIELDS
    // (null/0 for cricket — frontend ignore karega)
    // ═══════════════════════════════════
    private Integer team1Goals;          // Team 1 ka total goals
    private Integer team2Goals;          // Team 2 ka total goals
    private Integer half;                // 1 = First Half, 2 = Second Half
    private Long scorerPlayerId;         // Goal scorer
    private Long assistPlayerId;         // Assist dene wala
    private String futsalEventType;      // GOAL, OWN_GOAL, PENALTY, YELLOW_CARD, RED_CARD, HALF_END, MATCH_END
    private Long scoringTeamId;          // Kis team ne goal kiya
    private Integer matchMinute;         // Minute of event (e.g. 23')
    private List<FutsalEventDTO> futsalEvents; // Live events list (frontend ke liye)

    private List<PlayerSimpleDTO> availableBatters;
    private List<PlayerSimpleDTO> availableBowlers;
    private boolean matchEnd = false;
}




