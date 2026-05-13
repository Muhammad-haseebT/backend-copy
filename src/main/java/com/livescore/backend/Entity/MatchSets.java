package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class MatchSets {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MatchSets -> Match
    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference("match-sets")
    private Match match;

    private int setNumber;
    private int team1Score;
    private int team2Score;

    // winnerTeam: reference Team (no JSON backref)
    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    @JsonIgnore
    private Team winnerTeam;

    // MatchSets -> SetPoint (one-to-many)
    @OneToMany(mappedBy = "matchSet", cascade = CascadeType.ALL)
    @JsonManagedReference("matchSet-points")
    private List<SetPoint> points = new ArrayList<>();
}
