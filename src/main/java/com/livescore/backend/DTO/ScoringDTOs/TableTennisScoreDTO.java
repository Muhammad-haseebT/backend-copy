package com.livescore.backend.DTO.ScoringDTOs;

import com.livescore.backend.DTO.PlayerSimpleDTO;
import lombok.Data;
import java.util.List;

@Data
public class TableTennisScoreDTO {

    // ── Request ───────────────────────────────────────────────────
    private Long    matchId;
    private Long    teamId;
    private Long    playerId;
    private String  eventType;
    private boolean undo;

    // ── Response ──────────────────────────────────────────────────
    private Integer team1Points;
    private Integer team2Points;
    private Integer team1Games;
    private Integer team2Games;
    private Integer currentGame;
    private String  status;

    private Integer gamesToWin;
    private Integer pointsPerGame;
    private Integer maxPoints;      // 0 = no cap
    private Integer pointsToWin;   // dynamic: 11, or higher at deuce

    private Long    gameStartTime;

    private List<TableTennisEventDTO> tableTennisEvents;
    private String comment;
    private List<PlayerSimpleDTO> team1Players;   // playing players (1 or 2)
    private List<PlayerSimpleDTO> team2Players;
}
