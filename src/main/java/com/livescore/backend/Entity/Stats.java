package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Data
public class Stats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    @JsonBackReference("tournament-stats")
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "player_id")
    @JsonIgnore
    private Player player;

    @ManyToOne
    @JoinColumn(name = "sport_type_id")
    @JsonIgnore
    private Sports sportType;

    // ========== BATTING ==========
    private Integer runs;
    private Integer ballsFaced;
    private Integer fours;
    private Integer sixes;
    private Integer highest;
    private Integer fifties;
    private Integer hundreds;
    private Integer notOut;
    private Integer inningsPlayed;
    private Integer strikeRate;
    private Double battingAverage;

    // ========== BOWLING ==========
    private Integer wickets;
    private Integer ballsBowled;
    private Integer runsConceded;
    private Integer maidens;
    private Integer threeWicketHauls;
    private Integer fiveWicketHauls;
    private Integer dotBalls;
    private Double economy;
    private Double bowlingAverage;
    private Double bowlingStrikeRate;

    // ========== FIELDING ==========
    private Integer catches;
    private Integer runouts;
    private Integer stumpings;

    // ========== AWARDS ==========
    private Integer playerOfMatchCount;
    private Integer points;

    // ========== FOOTBALL ==========
    private Integer goals;
    private Integer assists;
    private Integer fouls;
    private Integer yellowCards;
    private Integer redCards;

    @PrePersist
    public void prePersist() {
        if (runs == null) runs = 0;
        if (wickets == null) wickets = 0;
        if (strikeRate == null) strikeRate = 0;
        if (battingAverage == null) battingAverage = 0.0;
        if (highest == null) highest = 0;
        if (notOut == null) notOut = 0;
        if (points == null) points = 0;
        if (goals == null) goals = 0;
        if (assists == null) assists = 0;
        if (fouls == null) fouls = 0;
        if (yellowCards == null) yellowCards = 0;
        if (redCards == null) redCards = 0;
        if (ballsFaced == null) ballsFaced = 0;
        if (ballsBowled == null) ballsBowled = 0;
        if (runsConceded == null) runsConceded = 0;
        if (fours == null) fours = 0;
        if (sixes == null) sixes = 0;
        if (fifties == null) fifties = 0;
        if (hundreds == null) hundreds = 0;
        if (inningsPlayed == null) inningsPlayed = 0;
        if (maidens == null) maidens = 0;
        if (threeWicketHauls == null) threeWicketHauls = 0;
        if (fiveWicketHauls == null) fiveWicketHauls = 0;
        if (dotBalls == null) dotBalls = 0;
        if (catches == null) catches = 0;
        if (runouts == null) runouts = 0;
        if (stumpings == null) stumpings = 0;
        if (playerOfMatchCount == null) playerOfMatchCount = 0;
        if (economy == null) economy = 0.0;
        if (bowlingAverage == null) bowlingAverage = 0.0;
        if (bowlingStrikeRate == null) bowlingStrikeRate = 0.0;
    }
}