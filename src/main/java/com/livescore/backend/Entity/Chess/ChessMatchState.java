package com.livescore.backend.Entity.Chess;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity @Data @Table(name = "chess_match_states")
public class ChessMatchState {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", unique = true)
    private Match match;

    @Column(name = "team1_checks") private Integer team1Checks = 0;
    @Column(name = "team2_checks") private Integer team2Checks = 0;

    @Column(name = "status") private String status = "LIVE";
    @Column(name = "result_type") private String resultType;
    @Column(name = "match_start_time") private Long matchStartTime;
}
