package com.livescore.backend.DTO;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class FixtureRequestDTO {
    private String tournamentType; // ROUND_ROBIN, LEAGUE, KNOCK_OUT, MIXED
    private LocalDate startDate;
    private LocalTime startTime;
    private Integer gapMinutes;
    private String venue;
    private Integer overs;
    private String scorerId;
    private String mediaScorerUsername;
}
