package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;
import java.util.List;

@Data
public class TugOfWarScoreDTO {

    // ── Request ───────────────────────────────────────────────────
    private Long    matchId;
    private Long    winnerTeamId; // team that won this round
    private String  eventType;   // ROUND_WIN, END_MATCH
    private boolean undo;

    // ── Response ──────────────────────────────────────────────────
    private Integer team1Rounds;  // rounds won by team1
    private Integer team2Rounds;  // rounds won by team2
    private Integer currentRound;
    private Integer roundsToWin;  // e.g. 3 (best of 5)
    private Integer totalRounds;  // roundsToWin * 2 - 1
    private String  status;       // LIVE, ROUND_BREAK, COMPLETED

    private Long    roundStartTime;

    private List<TugOfWarEventDTO> tugOfWarEvents;
    private String comment;
}
