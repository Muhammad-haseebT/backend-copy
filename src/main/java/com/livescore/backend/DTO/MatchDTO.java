package com.livescore.backend.DTO;

import com.livescore.backend.Entity.Sports;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class MatchDTO {
    private Long id;

    private Long tournamentId;
    private String tournamentName;

    private Long team1Id;
    private String team1Name;

    private Long team2Id;
    private String team2Name;

    private String scorerId;
    private String mediaScorerUsername;
    private String status;
    private String venue;
    private LocalDate date;
    private LocalTime time;

    private Long tossWinnerId;
    private String decision;
    private Long winnerTeamId;
    private String winnerTeamName;
    private Long sportId;
    private int overs;
    private int sets;
    private Long InningsId;
    private Integer pointsPerSet;
    private Integer finalSetPoints;
    private List<Long> team1PlayingIds;
    private List<Long> team2PlayingIds;
    private Integer halfDurationMins;

    // FIX: format for Ludo ("1v1"/"2v2") and Chess ("1v1"/"2v2")
    // Saved to DB at match start, read back when match is LIVE/COMPLETED
    private String matchFormat;
}