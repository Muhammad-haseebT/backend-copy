package com.livescore.backend.Entity.TableTennis;

import com.livescore.backend.Entity.Match;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "tabletennis_match_states")
public class TableTennisMatchState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", unique = true)
    private Match match;

    // ── Current game points ───────────────────────────────────────
    @Column(name = "team1_points") private Integer team1Points = 0;
    @Column(name = "team2_points") private Integer team2Points = 0;

    // ── Games won ────────────────────────────────────────────────
    @Column(name = "team1_games") private Integer team1Games = 0;
    @Column(name = "team2_games") private Integer team2Games = 0;

    @Column(name = "current_game") private Integer currentGame = 1;

    // ── Config ───────────────────────────────────────────────────
    // TT standard: best of 7, 11 pts, win by 2, no cap
    @Column(name = "games_to_win")    private Integer gamesToWin    = 4; // best of 7 → 4
    @Column(name = "points_per_game") private Integer pointsPerGame = 11;
    // Max points: 0 = no cap (deuce goes on indefinitely, win by 2)
    @Column(name = "max_points")      private Integer maxPoints     = 0;

    // ── Status: LIVE, GAME_BREAK, COMPLETED ──────────────────────
    @Column(name = "status") private String status = "LIVE";

    @Column(name = "game_start_time") private Long gameStartTime;

    @Column(name = "team1_player_ids", length = 200)
    private String team1PlayerIds;

    @Column(name = "team2_player_ids", length = 200)
    private String team2PlayerIds;

}
