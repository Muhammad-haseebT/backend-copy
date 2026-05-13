package com.livescore.backend.Entity.Ludo;

import com.livescore.backend.Entity.Match;
import jakarta.persistence.*;
import lombok.Data;

@Entity @Data @Table(name = "ludo_match_states")
public class LudoMatchState {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", unique = true)
    private Match match;

    @Column(name = "team1_home_runs") private Integer team1HomeRuns = 0;
    @Column(name = "team2_home_runs") private Integer team2HomeRuns = 0;

    // 4 for 1v1, 8 for 2v2 — set on first HOME_RUN if not already set
    @Column(name = "max_home_runs") private Integer maxHomeRuns = 4;

    @Column(name = "status") private String status = "LIVE";
    @Column(name = "match_start_time") private Long matchStartTime;
}