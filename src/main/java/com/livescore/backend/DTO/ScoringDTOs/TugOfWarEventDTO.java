package com.livescore.backend.DTO.ScoringDTOs;

import lombok.Data;

@Data
public class TugOfWarEventDTO {
    private Long    id;
    private String  eventType;
    private Integer roundNumber;
    private Integer eventTimeSeconds;
    private Integer roundDurationSeconds;
    private Long    winnerTeamId;
    private String  winnerTeamName;
}
