package com.livescore.backend.DTO.ScoringDTOs;

import com.livescore.backend.DTO.PlayerSimpleDTO;
import lombok.Data;
import java.util.List;

@Data
public class VolleyballScoreDTO {

    // ── Request ───────────────────────────────────────────────────
    private Long    matchId;
    private Long    teamId;
    private Long    playerId;
    private Long    outPlayerId;
    private Long    inPlayerId;
    private String  eventType;
    private boolean undo;

    // ── Response ──────────────────────────────────────────────────
    private Integer team1Points;
    private Integer team2Points;
    private Integer team1Sets;
    private Integer team2Sets;
    private Integer currentSet;
    private String  status;

    private Integer team1Timeouts;
    private Integer team2Timeouts;
    private Long    setStartTime;

    // ✅ Config fields — frontend uses these for circles + progress
    private Integer setsToWin;      // e.g. 3
    private Integer pointsToWin;    // current set target (25 or 15)
    private Integer pointsPerSet;
    private Integer finalSetPoints;

    private List<VolleyballEventDTO> volleyballEvents;
    private String comment;
    private List<PlayerSimpleDTO> team1Players;   // full approved squad
    private List<PlayerSimpleDTO> team2Players;
    private List<PlayerSimpleDTO> team1OnField;   // currently on field
    private List<PlayerSimpleDTO> team2OnField;
}