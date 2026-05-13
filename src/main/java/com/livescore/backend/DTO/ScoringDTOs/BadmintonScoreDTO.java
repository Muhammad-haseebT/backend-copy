package com.livescore.backend.DTO.ScoringDTOs;

import com.livescore.backend.DTO.PlayerSimpleDTO;
import lombok.Data;
import java.util.List;

@Data
public class BadmintonScoreDTO {

    // ── Request ───────────────────────────────────────────────────
    private Long    matchId;
    private Long    teamId;
    private Long    playerId;
    private Long    outPlayerId;
    private Long    inPlayerId;
    // POINT, SMASH, SERVICE_ACE, NET_FAULT, FOOT_FAULT,
    // OUT, SUBSTITUTION, END_GAME
    private String  eventType;
    private boolean undo;

    // ── Response ──────────────────────────────────────────────────
    private Integer team1Points;
    private Integer team2Points;
    private Integer team1Games;     // games won
    private Integer team2Games;
    private Integer currentGame;
    private String  status;         // LIVE, GAME_BREAK, COMPLETED

    // Config
    private Integer gamesToWin;
    private Integer pointsPerGame;
    private Integer maxPoints;
    private Integer pointsToWin;    // current game target

    private Long    gameStartTime;

    private List<BadmintonEventDTO> badmintonEvents;
    private String comment;
    private List<PlayerSimpleDTO> team1Players;   // playing players (1 or 2)
    private List<PlayerSimpleDTO> team2Players;
}
