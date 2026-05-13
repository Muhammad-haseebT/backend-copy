package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.springframework.web.socket.WebSocketExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@ToString(exclude = {"tournament", "team1", "team2", "scorer", "tossWinner", "winnerTeam",
        "manOfMatch", "bestBatsman", "bestBowler", "cricketInnings",
        "footballEvents", "matchSets", "boards", "mediaList", "cricketBalls"})
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    @JsonBackReference("tournament-matches")
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "team1_id")
    @JsonIgnore
    private Team team1;

    @ManyToOne
    @JoinColumn(name = "team2_id")
    @JsonIgnore
    private Team team2;

    @ManyToOne
    @JoinColumn(name = "scorer_id")
    @JsonBackReference("account-scoredMatches")
    private Account scorer;

    @ManyToOne
    @JoinColumn(name = "media_scorer_id")
    @JsonIgnore
    private Account mediaScorer;

    private String status; // UPCOMING / LIVE / FINISHED / COMPLETED
    private String venue;
    private LocalDate date;
    private LocalTime time;

    @Column(name = "points_per_set")
    private Integer pointsPerSet;   // 25 for volleyball, 21 for badminton

    @Column(name = "final_set_points")
    private Integer finalSetPoints;

    // FIX: store match format for Ludo ("1v1"/"2v2") and Chess ("1v1"/"2v2")
    @Column(name = "match_format")
    private String matchFormat;

    @PrePersist
    public void prePersist() {
        if (this.status == null || this.status.isBlank()) {
            this.status = "UPCOMING";
        } else {
            this.status = this.status.toUpperCase();
        }
        if (this.decision != null) {
            this.decision = this.decision.toUpperCase();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.status != null) {
            this.status = this.status.toUpperCase();
        }
        if (this.decision != null) {
            this.decision = this.decision.toUpperCase();
        }
    }

    @ManyToOne
    @JoinColumn(name = "toss_winner_id")
    @JsonIgnore
    private Team tossWinner;

    private int overs;
    private int sets;

    private String decision;

    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    @JsonIgnore
    private Team winnerTeam;

    @ManyToOne
    @JoinColumn(name = "man_of_match_id")
    @JsonIgnore
    private Player manOfMatch;

    @ManyToOne
    @JoinColumn(name = "best_batsman_id")
    @JsonIgnore
    private Player bestBatsman;

    @ManyToOne
    @JoinColumn(name = "best_bowler_id")
    @JsonIgnore
    private Player bestBowler;

    @Column(name = "team1_playing_ids", length = 500)
    private String team1PlayingIds;

    @Column(name = "team2_playing_ids", length = 500)
    private String team2PlayingIds;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-innings")
    private List<CricketInnings> cricketInnings = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-goals")
    private List<GoalsType> footballEvents = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-sets")
    private List<MatchSets> matchSets = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-boards")
    private List<Board> boards = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-media")
    @JsonIgnore
    private List<Media> mediaList = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    @JsonManagedReference("match-balls")
    @JsonIgnore
    private List<CricketBall> cricketBalls = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "fav_player_id")
    @JsonIgnore
    private FavouritePlayer favouritePlayer;

    @Column(name = "half_duration_mins")
    private Integer halfDurationMins;
}