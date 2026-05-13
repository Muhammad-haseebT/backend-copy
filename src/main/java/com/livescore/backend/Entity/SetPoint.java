package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class SetPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SetPoint -> MatchSets
    @ManyToOne
    @JoinColumn(name = "match_set_id")
    @JsonBackReference("matchSet-points")
    private MatchSets matchSet;

    // SetPoint -> Team
    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonBackReference("team-points")
    private Team team;

    @Column(nullable = false)
    private String pointType; // normal / ace / serviceError etc.
}
