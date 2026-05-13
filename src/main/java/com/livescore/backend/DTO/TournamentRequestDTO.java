package com.livescore.backend.DTO;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TournamentRequestDTO {
    private String name;
    private String username;
    private String playerType;
    private String tournamentType;
    private String tournamentStage;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long seasonId;
    private Long sportsId;
}
