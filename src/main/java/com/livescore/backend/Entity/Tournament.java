package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
// Tournament.java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {
        "organizer",
        "season",
        "teams",
        "matches",
        "sport",
        "stats"
})
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private LocalDate startDate;
    private LocalDate endDate;

    private String playerType;//male/female
    private String tournamentType;//hard,tennis etc
    private String tournamentStage;//round-robin,knock out

    // Tournament -> Account (organizer) many-to-one
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    @JsonIgnore
    private Account organizer;

    // Tournament -> Sports (many-to-one)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sports_id")
    @JsonIgnore
    private Sports sport;

    // Tournament -> Season (many-to-one)
    @ManyToOne
    @JoinColumn(name = "season_id")
    @JsonIgnore
    private Season season;

    // Tournament -> Team (one-to-many)
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Team> teams = new ArrayList<>();

    // Tournament -> Match (one-to-many)
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Match> matches = new ArrayList<>();

    // Tournament -> Stats (one-to-many)
    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Stats> stats = new ArrayList<>();


}
