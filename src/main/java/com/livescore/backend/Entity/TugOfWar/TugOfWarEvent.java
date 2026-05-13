package com.livescore.backend.Entity.TugOfWar;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "tugofwar_events")
public class TugOfWarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    // Team that won this round (null for START_ROUND, END_MATCH)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private Team winnerTeam;

    // ROUND_WIN   — a team won this round
    // START_ROUND — new round started
    // END_MATCH   — match manually ended
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "round_number")
    private Integer roundNumber;

    @Column(name = "event_time_seconds")
    private Integer eventTimeSeconds;

    // How long the round lasted (in seconds)
    @Column(name = "round_duration_seconds")
    private Integer roundDurationSeconds;
}
