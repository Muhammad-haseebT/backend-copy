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
    private int    runsScored;      // formerly totalRuns
    private int    ballsFaced;
    private double strikeRate;
    private double average;         // formerly battingAvg
    private int    highestScore;    // formerly highest
    private int    fours;
    private int    sixes;
    private int    notOuts;
    private int    fifties;
    private int    hundreds;

    // ── Cricket — Bowling ────────────────────────────────────────
    private int    wicketsTaken;    // formerly wickets
    private int    ballsBowled;
    private int    runsConceded;
    private double economy;
    private double bowlingAverage;
    private double bowlingStrikeRate;
    private String bestBowling;     // ✅ new field
    private int    maidens;
    private int    dotBalls;
    private int    threeWicketHauls;
    private int    fiveWicketHauls;

    // ── Cricket — Fielding ───────────────────────────────────────
    private int catches;
    private int stumpings;
    private int runouts;

    // ── Multi-sport (futsal/volleyball/badminton/tt/ludo/chess) ──
    private int goals;
    private int assists;
    private int ownGoals;
    private int futsalFouls;
    private int yellowCards;
    private int redCards;
}