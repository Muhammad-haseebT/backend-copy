package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;
import java.util.List;

@Data
public class LudoScoreDTO {
    // Request fields
    private Long    matchId;
    private Long    teamId;
    private Long    playerId;
    // HOME_RUN, WIN, END_MATCH
    private String  eventType;
    private boolean undo;
    // 4 for 1v1, 8 for 2v2 — frontend sends this so backend knows the format
    private Integer maxHomeRuns;

    // Response fields
    private Integer team1HomeRuns;
    private Integer team2HomeRuns;
    private String  status;
    private Long    matchStartTime;
    private List<LudoEventDTO> ludoEvents;
    private String  comment;

    // FIX: added so frontend knows which team won (used for winner/loser banner)
    private Long    winnerTeamId;
}