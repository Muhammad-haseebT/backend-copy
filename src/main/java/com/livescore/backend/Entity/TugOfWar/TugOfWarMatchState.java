package com.livescore.backend.Entity.TugOfWar;

import com.livescore.backend.Entity.Match;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "tugofwar_match_states")
public class TugOfWarMatchState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", unique = true)
    private Match match;

    // ── Rounds won ───────────────────────────────────────────────
    @Column(name = "team1_rounds") private Integer team1Rounds = 0;
    @Column(name = "team2_rounds") private Integer team2Rounds = 0;

    @Column(name = "current_round") private Integer currentRound = 1;

    // ── Config ───────────────────────────────────────────────────
    // Best of X rounds — from match.sets (rounds to win e.g. 3 for best of 5)
    @Column(name = "rounds_to_win") private Integer roundsToWin = 3;

    // ── Status: LIVE, ROUND_BREAK, COMPLETED ─────────────────────
    @Column(name = "status") private String status = "LIVE";

    @Column(name = "round_start_time") private Long roundStartTime;
}
