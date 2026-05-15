package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class TeamStatsResponseDTO {

    // ── Identity ──────────────────────────────────────────────────────────
    private Long   teamId;
    private String teamName;
    private String sport;       // "cricket" / "futsal" / "volleyball" etc.
    private Long   sportId;

    // ── Match Record (from PtsTable) ──────────────────────────────────────
    private int    matchesPlayed;
    private int    wins;
    private int    losses;
    private int    draws;
    private Double nrr;           // cricket / futsal NRR

    // ── Cricket totals (sum of all players in team) ───────────────────────
    private int    totalRunsScored;
    private int    totalWicketsTaken;
    private int    totalFours;
    private int    totalSixes;
    private int    highestTeamScore;   // best individual innings in tournament
    private int    totalCatches;

    // ── Futsal totals ─────────────────────────────────────────────────────
    private int    totalGoals;
    private int    totalAssists;
    private int    totalFouls;
    private int    totalYellowCards;
    private int    totalRedCards;
    // goalsFor / goalsAgainst from PtsTable
    private int    goalsFor;
    private int    goalsAgainst;

    // ── Volleyball totals ─────────────────────────────────────────────────
    // goals  → points scored
    // assists→ aces
    // fouls  → blocks
    // yellow → attack errors
    // red    → service errors
    // (same Stats fields reused — consistent with FutsalStatsService pattern)

    // ── Badminton / Table Tennis totals ───────────────────────────────────
    // goals  → points scored
    // assists→ smashes + aces
    // fouls  → faults
    // yellow → out shots

    // ── Ludo totals ───────────────────────────────────────────────────────
    // goals  → home runs
    // assists→ captures

    // ── Chess totals ──────────────────────────────────────────────────────
    // wins   → (from PtsTable)
    // assists→ checks delivered

    // ── Tug of War totals ─────────────────────────────────────────────────
    // wins / losses from PtsTable
    // goals → rounds won

    // ── Top Performer ─────────────────────────────────────────────────────
    private Long   topScorerPlayerId;
    private String topScorerName;
    private String topScorerStat;    // e.g. "120 runs" / "8 goals" / "3 wins"
}