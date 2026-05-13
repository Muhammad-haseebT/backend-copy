package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@ToString(exclude = {"match", "team", "balls"})  // ✅ Add this
public class CricketInnings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int no; // 1 or 2

    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference("match-innings")
    private Match match;

    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonIgnore
    private Team team;

    @Column(name = "is_super_over", nullable = false)
    private boolean isSuper_Over = false;

    @OneToMany(mappedBy = "innings", cascade = CascadeType.ALL)
    @JsonManagedReference("innings-balls")
    private List<CricketBall> balls = new ArrayList<>();
}
