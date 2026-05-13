package com.livescore.backend.Entity.Volleyball;

import com.livescore.backend.Entity.Match;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "volleyball_match_states")
public class VolleyballMatchState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", unique = true)
    private Match match;

    // ── Current set points ───────────────────────────────────────
    @Column(name = "team1_points") private Integer team1Points = 0;
    @Column(name = "team2_points") private Integer team2Points = 0;

    // ── Sets won (match score) ────────────────────────────────────
    @Column(name = "team1_sets") private Integer team1Sets = 0;
    @Column(name = "team2_sets") private Integer team2Sets = 0;

    // ── Current set number (1 to totalSets*2-1) ──────────────────
    @Column(name = "current_set") private Integer currentSet = 1;

    // ── Config — read from Match on init ─────────────────────────
    @Column(name = "sets_to_win")    private Integer setsToWin    = 3;  // e.g. 3 (best of 5)
    @Column(name = "points_per_set") private Integer pointsPerSet = 25; // e.g. 25
    @Column(name = "final_set_pts")  private Integer finalSetPoints= 15; // e.g. 15 (tiebreak set)

    // ── Status ───────────────────────────────────────────────────
    @Column(name = "status") private String status = "LIVE";

    // ── Timeouts (each team 2 per set) ───────────────────────────
    @Column(name = "team1_timeouts") private Integer team1Timeouts = 0;
    @Column(name = "team2_timeouts") private Integer team2Timeouts = 0;

    @Column(name = "set_start_time") private Long setStartTime;
    private Integer sets;
    @Column(name = "team1_on_field_ids", length = 500)
    private String team1OnFieldIds;

    @Column(name = "team2_on_field_ids", length = 500)
    private String team2OnFieldIds;
}
