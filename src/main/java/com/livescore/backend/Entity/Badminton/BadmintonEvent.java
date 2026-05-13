package com.livescore.backend.Entity.Badminton;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "badminton_events")
public class BadmintonEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // Player who scored/faulted
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    // Substitution: coming in (doubles only)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_player_id")
    private Player inPlayer;

    // POINT          — team wins rally (no specific shot type)
    // SMASH          — point via smash (also adds +1 point to team)
    // SERVICE_ACE    — direct point from serve
    // NET_FAULT      — player's net fault → opponent gets point
    // FOOT_FAULT     — serve fault → opponent gets point
    // OUT            — shuttle out → opponent gets point
    // SUBSTITUTION   — doubles player change
    // END_GAME       — manual game end
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "game_number")
    private Integer gameNumber;

    @Column(name = "event_time_seconds")
    private Integer eventTimeSeconds;

    // Score snapshot at this event: "15-12"
    @Column(name = "score_snapshot")
    private String scoreSnapshot;
}
