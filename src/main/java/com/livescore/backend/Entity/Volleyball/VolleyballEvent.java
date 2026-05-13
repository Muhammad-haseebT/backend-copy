package com.livescore.backend.Entity.Volleyball;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "volleyball_events")
public class VolleyballEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // Player who performed the action (optional for POINT)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    // POINT, ACE, BLOCK, ATTACK_ERROR, SERVICE_ERROR,
    // SUBSTITUTION, TIMEOUT, END_SET
    @Column(name = "event_type", nullable = false)
    private String eventType;

    // Which set this happened in
    @Column(name = "set_number")
    private Integer setNumber;

    // Seconds elapsed since set start
    @Column(name = "event_time_seconds")
    private Integer eventTimeSeconds;

    // Substitution: player coming in
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_player_id")
    private Player inPlayer;
}
