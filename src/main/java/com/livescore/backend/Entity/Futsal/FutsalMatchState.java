package com.livescore.backend.Entity.Futsal;

import com.livescore.backend.Entity.Match;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "futsal_match_states")
public class FutsalMatchState {

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

    // Fouls — resets each half in futsal
    @Column(name = "team1_fouls")
    private Integer team1Fouls = 0;

    @Column(name = "team2_fouls")
    private Integer team2Fouls = 0;

    // Cards — do NOT reset
    @Column(name = "team1_yellow_cards")
    private Integer team1YellowCards = 0;

    @Column(name = "team2_yellow_cards")
    private Integer team2YellowCards = 0;

    @Column(name = "team1_red_cards")
    private Integer team1RedCards = 0;

    @Column(name = "team2_red_cards")
    private Integer team2RedCards = 0;

    // 1 = first half, 2 = second half, 3 = extra time
    @Column(name = "current_half")
    private Integer currentHalf = 1;

    // LIVE, HALF_TIME, EXTRA_TIME, COMPLETED
    @Column(name = "status")
    private String status = "LIVE";

    @Column(name = "in_extra_time")
    private Boolean inExtraTime = false;

    // epoch ms — for server-synced frontend timer
    @Column(name = "half_start_time")
    private Long halfStartTime;

    // 25 for regular halves, 5 for extra time
    @Column(name = "half_duration_minutes")
    private Integer halfDurationMinutes = 25;

    @Column(name = "team1_on_field_ids", length = 500)
    private String team1OnFieldIds;

    @Column(name = "team2_on_field_ids", length = 500)
    private String team2OnFieldIds;
}