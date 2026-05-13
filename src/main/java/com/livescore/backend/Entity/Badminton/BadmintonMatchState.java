package com.livescore.backend.Entity.Badminton;

import com.livescore.backend.Entity.Match;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "badminton_match_states")
public class BadmintonMatchState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", unique = true)
    private Match match;

    // ── Current game points ───────────────────────────────────────
    @Column(name = "team1_points") private Integer team1Points = 0;
    @Column(name = "team2_points") private Integer team2Points = 0;

    // ── Games won (match score) ───────────────────────────────────
    @Column(name = "team1_games") private Integer team1Games = 0;
    @Column(name = "team2_games") private Integer team2Games = 0;

    // ── Current game number ───────────────────────────────────────
    @Column(name = "current_game") private Integer currentGame = 1;

    // ── Config (from Match entity) ────────────────────────────────
    @Column(name = "games_to_win")   private Integer gamesToWin   = 2; // best of 3 → 2
    @Column(name = "points_per_game")private Integer pointsPerGame= 21; // standard
    // Max points: if 29-29, next point wins (30 is cap)
    @Column(name = "max_points")     private Integer maxPoints    = 30;

    // ── Status: LIVE, GAME_BREAK, COMPLETED ──────────────────────
    @Column(name = "status") private String status = "LIVE";

    // ── Timer ────────────────────────────────────────────────────
    @Column(name = "game_start_time") private Long gameStartTime;
    // Singles = 1 ID, Doubles = 2 IDs (no bench concept)
    @Column(name = "team1_player_ids", length = 200)
    private String team1PlayerIds;

    @Column(name = "team2_player_ids", length = 200)
    private String team2PlayerIds;
}
