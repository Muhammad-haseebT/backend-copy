package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class GoalsType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // GoalsType -> Match
    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference("match-goals")
    private Match match;

    // player / assist -> Player (one-way JSON)
    @ManyToOne
    @JoinColumn(name = "player_id")
    @JsonIgnore
    private Player player;

    @ManyToOne
    @JoinColumn(name = "assist_id")
    @JsonIgnore
    private Player assistBy;

    private int minute;
    private int goal;
    private int foul;
    private int yellow;
    private int red;
    private int assist;
}
