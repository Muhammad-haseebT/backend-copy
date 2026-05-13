package com.livescore.backend.DTO;

import lombok.Data;
import java.util.List;

@Data
public class TournamentAwardsDTO {

    private Long   tournamentId;
    private String tournamentName;
    private String sport; // "cricket" / "futsal" — frontend uses this

    // ── Awards (work for both sports) ────────────────────────────
    private List<AwardDTO> manOfTournament;       // top 3, admin-set
    private AwardDTO       favouritePlayer;
    private List<AwardDTO> allAwards; // per-match POMs

    // ── Cricket-specific awards ───────────────────────────────────
    private AwardDTO bestBatsman;
    private AwardDTO bestBowler;
    private AwardDTO bestFielder;
    private AwardDTO mostSixes;

    // ── Cricket leaderboards ──────────────────────────────────────
    private List<PlayerStatsRow> topRunScorers;
    private List<PlayerStatsRow> topBowlers;

    // ── Futsal-specific awards ────────────────────────────────────
    private AwardDTO topScorer;   // most goals
    private AwardDTO topAssist;   // most assists

    // ── Futsal leaderboards ───────────────────────────────────────
    private List<PlayerStatsRow> topGoalScorers;
    private List<PlayerStatsRow> topAssisters;

    // ─────────────────────────────────────────────────────────────
    // Inner DTOs
    // ─────────────────────────────────────────────────────────────

    @Data
    public static class AwardDTO {
        private Long   playerId;
        private String playerName;
        private String awardType;
        private int    points;
        private String reason;
    }

    @Data
    public static class PlayerStatsRow {

        private Long   playerId;
        private String playerName;

        // ── Cricket batting ──────────────────────────────────────
        private Integer runs;
        private Integer ballsFaced;
        private Integer fours;
        private Integer sixes;
        private Integer highest;
        private Integer strikeRate;
        private Integer fifties;
        private Integer hundreds;

        // ── Cricket bowling ──────────────────────────────────────
        private Integer wickets;
        private Integer ballsBowled;
        private Integer runsConceded;
        private Double  economy;
        private Double  bowlingAverage;
        private Integer maidens;
        private Integer dotBalls;

        // ── Cricket fielding ─────────────────────────────────────
        private Integer catches;
        private Integer stumpings;
        private Integer runouts;

        // ── Futsal ───────────────────────────────────────────────
        private Integer goals;
        private Integer assists;
        private Integer futsalFouls;
        private Integer yellowCards;
        private Integer redCards;

        // ── Shared ───────────────────────────────────────────────
        private Integer playerOfMatchCount;
        private Integer totalPoints;
    }
}