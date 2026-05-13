package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Entity
@Data
@Table(name = "pts_table")
@NoArgsConstructor

public class PtsTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PtsTable.java
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    @JsonIgnore   // keep this
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    @JsonIgnore
    private Tournament tournament;

    // ── Common fields (cricket + futsal) ─────────────────────────
    @Column(name = "played")
    private Integer played = 0;

    @Column(name = "wins")
    private Integer wins = 0;

    @Column(name = "losses")
    private Integer losses = 0;

    @Column(name = "points")
    private Integer points = 0;

    // ── Cricket specific ─────────────────────────────────────────
    // NRR = Net Run Rate
    @Column(name = "nrr")
    private Double nrr = 0.0;

    // ── Futsal specific ──────────────────────────────────────────
    // Draws (futsal has draws, cricket doesn't in tournament format)
    @Column(name = "draws")
    private Integer draws = 0;

    // Goals for (scored by this team)
    @Column(name = "goals_for")
    private Integer goalsFor = 0;

    // Goals against (conceded by this team)
    @Column(name = "goals_against")
    private Integer goalsAgainst = 0;

    public PtsTable(Team team, Tournament tournament) {
        this.team = team;
        this.tournament = tournament;
    }

}