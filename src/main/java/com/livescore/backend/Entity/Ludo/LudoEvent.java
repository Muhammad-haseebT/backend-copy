package com.livescore.backend.Entity.Ludo;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "ludo_events")
public class LudoEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    // CAPTURE   — team captures opponent piece (+1 capture to team)
    // HOME_RUN  — team gets a piece to home (+1 home run)
    // WIN       — team wins the match (auto-triggered)
    // END_MATCH — manually end match
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_time_seconds")
    private Integer eventTimeSeconds;
}
