package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class PlayerInnings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "innings_id",unique = false)
    @JsonIgnore
    private CricketInnings innings;

    @ManyToOne
    @JoinColumn(name = "player_id")
    @JsonIgnore
    private Player player;
    private String role="Batsman";


    private  int runs;
    private int ballsFaced;
    private int ballsBowled;
    private int runsConceded;
    private int four;

    private int sixes;
    private int wickets;

    private double rr;
    private double eco;

    @Column(name = "is_dismissed")  // keeps same DB column name
    private Boolean dismissed;
    private String dismissalType;
    private Boolean isOnCrease = false;
    private Boolean isStriker = false;
    private Boolean isCurrentBowler = false;

    private int overs = 0;
}
