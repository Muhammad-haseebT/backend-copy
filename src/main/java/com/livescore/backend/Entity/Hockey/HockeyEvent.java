package com.livescore.backend.Entity.Hockey;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "hockey_events")
public class HockeyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // Primary player (scorer / fouler / player going OUT in sub)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    // Substitution: player coming IN
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_player_id")
    private Player inPlayer;

    // Goal assist player
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assist_player_id")
    private Player assistPlayer;

    // GOAL, OWN_GOAL, FOUL, GREEN_CARD, YELLOW_CARD, RED_CARD,
    // SUBSTITUTION, PENALTY_CORNER, PENALTY_STROKE, END_PERIOD, TIMEOUT
    @Column(name = "event_type", nullable = false)
    private String eventType;

    // NORMAL, FIELD_GOAL, PENALTY_CORNER, PENALTY_STROKE
    @Column(name = "goal_type")
    private String goalType;

    // For FOUL: null = simple foul, GREEN, YELLOW, RED
    @Column(name = "card_type")
    private String cardType;

    // 1 = first period, 2 = second period, 3 = third period, 4 = extra time
    @Column(name = "period")
    private Integer period;

    // Seconds elapsed since period start
    @Column(name = "event_time_seconds")
    private Integer eventTimeSeconds;

    // True if this event happened in extra time
    @Column(name = "is_extra_time")
    private Boolean extraTime = false;
}
