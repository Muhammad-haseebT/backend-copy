package com.livescore.backend.Entity.Hockey;

import com.livescore.backend.Entity.Match;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "hockey_match_states")
public class HockeyMatchState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", unique = true)
    private Match match;

    // Scores
    @Column(name = "team1_score")
    private Integer team1Score = 0;

    @Column(name = "team2_score")
    private Integer team2Score = 0;

    // Fouls
    @Column(name = "team1_fouls")
    private Integer team1Fouls = 0;

    @Column(name = "team2_fouls")
    private Integer team2Fouls = 0;

    // Cards — do NOT reset between periods
    @Column(name = "team1_yellow_cards")
    private Integer team1YellowCards = 0;

    @Column(name = "team2_yellow_cards")
    private Integer team2YellowCards = 0;

    @Column(name = "team1_red_cards")
    private Integer team1RedCards = 0;

    @Column(name = "team2_red_cards")
    private Integer team2RedCards = 0;

    // Green cards — hockey specific (2-min suspension)
    @Column(name = "team1_green_cards")
    private Integer team1GreenCards = 0;

    @Column(name = "team2_green_cards")
    private Integer team2GreenCards = 0;

    // Penalty corners — hockey specific
    @Column(name = "team1_penalty_corners")
    private Integer team1PenaltyCorners = 0;

    @Column(name = "team2_penalty_corners")
    private Integer team2PenaltyCorners = 0;

    // 1 = first period, 2 = second period, 3 = third period, 4 = extra time
    @Column(name = "current_period")
    private Integer currentPeriod = 1;

    // LIVE, BREAK, EXTRA_TIME, COMPLETED
    @Column(name = "status")
    private String status = "LIVE";

    @Column(name = "in_extra_time")
    private Boolean inExtraTime = false;

    // epoch ms — for server-synced frontend timer
    @Column(name = "period_start_time")
    private Long periodStartTime;

    // 15 min per regular period
    @Column(name = "period_duration_minutes")
    private Integer periodDurationMinutes = 15;

    @Column(name = "team1_on_field_ids", length = 500)
    private String team1OnFieldIds;

    @Column(name = "team2_on_field_ids", length = 500)
    private String team2OnFieldIds;
}
