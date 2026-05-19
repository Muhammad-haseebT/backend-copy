package com.livescore.backend.Entity.Hockey;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Per-match hockey stats for each player.
 *
 * Unlike Stats (tournament-level), this stores what a player did
 * in ONE specific match — useful for match-by-match progress tracking.
 *
 * Cricket equivalent: PlayerInnings
 */
@Entity
@Data
@Table(
        name = "hockey_match_stats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "player_id"})
)
public class HockeyMatchStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // ── Scoring ──────────────────────────────────────────────────
    @Column(name = "goals")
    private Integer goals = 0;

    @Column(name = "own_goals")
    private Integer ownGoals = 0;

    @Column(name = "assists")
    private Integer assists = 0;

    // ── Discipline ───────────────────────────────────────────────
    @Column(name = "fouls")
    private Integer fouls = 0;

    @Column(name = "green_cards")
    private Integer greenCards = 0;

    @Column(name = "yellow_cards")
    private Integer yellowCards = 0;

    @Column(name = "red_cards")
    private Integer redCards = 0;

    // ── Hockey Specific ──────────────────────────────────────────
    @Column(name = "penalty_corners")
    private Integer penaltyCorners = 0;

    // ── Derived ──────────────────────────────────────────────────
    // Fantasy points for this match
    @Column(name = "match_points")
    private Integer matchPoints = 0;

    // Was this player Player of the Match?
    @Column(name = "is_pom")
    private Boolean isPom = false;
}
