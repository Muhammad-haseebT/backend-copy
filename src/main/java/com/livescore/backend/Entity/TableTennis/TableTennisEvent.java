package com.livescore.backend.Entity.TableTennis;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "tabletennis_events")
public class TableTennisEvent {

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

    // POINT        — team wins rally
    // SMASH        — winner via smash (+point to team)
    // SERVICE_ACE  — direct ace (+point to team)
    // NET_FAULT    — net touch → opponent gets point
    // EDGE_BALL    — edge ball → point to team (legal in TT)
    // OUT          — ball out → opponent gets point
    // SERVICE_FAULT— serve fault → opponent gets point
    // END_GAME     — manual game end
    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "game_number")
    private Integer gameNumber;

    @Column(name = "event_time_seconds")
    private Integer eventTimeSeconds;

    @Column(name = "score_snapshot")
    private String scoreSnapshot;
}
