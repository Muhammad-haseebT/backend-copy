package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class PlayerFullStatsDTO {

    private Long   playerId;
    private String playerName;
    private String sport;

    // ✅ Each sport has its own count — matchesPlayed = only selected sport
    private int matchesPlayed;
    private int cricketMatchesPlayed;
    private int futsalMatchesPlayed;
    private int volleyballMatchesPlayed;
    private int badmintonMatchesPlayed;
    private int tableTennisMatchesPlayed;
    private int tugOfWarMatchesPlayed;
    private int ludoMatchesPlayed;
    private int chessMatchesPlayed;

    private int pomCount;

    // ── Cricket — Batting ────────────────────────────────────────
    private int    totalRuns;
    private int    ballsFaced;
    private double strikeRate;
    private double battingAvg;
    private int    highest;
    private int    fours;
    private int    sixes;
    private int    notOuts;
    private int    fifties;
    private int    hundreds;

    // ── Cricket — Bowling ────────────────────────────────────────
    private int    wickets;
    private int    ballsBowled;
    private int    runsConceded;
    private double economy;
    private double bowlingAverage;
    private double bowlingStrikeRate;
    private int    maidens;
    private int    dotBalls;
    private int    threeWicketHauls;
    private int    fiveWicketHauls;

    // ── Cricket — Fielding ───────────────────────────────────────
    private int catches;
    private int stumpings;
    private int runouts;

    // ── Multi-sport (futsal/volleyball/badminton/tt/ludo/chess) ──
    // goals       = points/goals/homeRuns/wins
    // assists     = assists/aces/smashes/captures/checks
    // futsalFouls = fouls/blocks/faults
    // yellowCards = yellow cards/attack errors/outs
    // redCards    = red cards/service errors
    private int goals;
    private int assists;
    private int ownGoals;
    private int futsalFouls;
    private int yellowCards;
    private int redCards;
}