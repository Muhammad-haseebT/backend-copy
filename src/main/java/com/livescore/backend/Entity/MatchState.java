package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class MatchState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "innings_id")
    @JsonIgnore
    private CricketInnings innings;
    @JoinColumn(name = "team_id")
    @ManyToOne
    @JsonIgnore
    private Team team;

    @JoinColumn(name = "striker_id")
    @ManyToOne
    @JsonIgnore
    private Player Striker;
    @JoinColumn(name = "non_striker_id")
    @ManyToOne
    @JsonIgnore
    private Player NonStriker;
    @JoinColumn(name = "bowler_id")
    @ManyToOne
    @JsonIgnore
    private Player bowler;



    private int runs;
    private int wickets;
    private int balls;
    private int overs;

    private double rr;
    private double crr;
    private int extras;
    private String status;
    private int target;

    private double requiredRR=0.0;
    @Column(name = "last_over_bowler_id")
    private Long lastOverBowlerId;
    @Column(nullable = true )
    private Boolean matchEnd=false;

}
