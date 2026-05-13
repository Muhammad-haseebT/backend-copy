package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;
import java.util.List;

@Data
public class ChessScoreDTO {
    // Request fields
    private Long   matchId;
    private Long   teamId;
    private Long   playerId;
    // Events: CHECKMATE, RESIGN, TIMEOUT, STALEMATE, DRAW_AGREED, END_MATCH
    private String eventType;
    private boolean undo;

    // Response fields
    private Integer team1Checks;
    private Integer team2Checks;
    private String  status;       // LIVE, COMPLETED
    private String  resultType;   // CHECKMATE, STALEMATE, RESIGN, TIMEOUT, DRAW_AGREED
    private Boolean isDraw;
    private Long    matchStartTime;
    private List<ChessEventDTO> chessEvents;
    private String  comment;

    // FIX: added so frontend can show winner/loser in completed banner
    private Long    winnerTeamId;
}