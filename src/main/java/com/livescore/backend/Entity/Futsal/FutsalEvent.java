package com.livescore.backend.Entity.Futsal;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "futsal_events")
public class FutsalEvent {

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

    // GOAL, OWN_GOAL, FOUL, YELLOW_CARD, RED_CARD,
    // SUBSTITUTION, END_HALF, EXTRA_TIME, TIMEOUT
    @Column(name = "event_type", nullable = false)
    private String eventType;

    // NORMAL, PENALTY, FREE_KICK, OWN_GOAL
    @Column(name = "goal_type")
    private String goalType;

    // For FOUL: null = simple foul, YELLOW, RED
    @Column(name = "card_type")
    private String cardType;

    // 1 = first half, 2 = second half, 3 = extra time
    @Column(name = "half")
    private Integer half;

    // Seconds elapsed since half/extra-time start
    @Column(name = "event_time_seconds")
    private Integer eventTimeSeconds;

    // True if this event happened in extra time
    @Column(name = "is_extra_time")
    private Boolean extraTime = false;
}