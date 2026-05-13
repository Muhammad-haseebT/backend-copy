package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@ToString(exclude = {"creator", "tournament", "players", "playerRequests", "pointsTableEntry"})
@EqualsAndHashCode(exclude = {"creator", "tournament", "players", "playerRequests", "pointsTableEntry"})
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Player creator;

    @ManyToOne
    @JoinColumn(name = "tournament_id")
    @JsonBackReference("tournament-teams")
    private Tournament tournament;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    @JsonManagedReference("team-playerTeams")
    private List<PlayerRequest> playerRequests = new ArrayList<>();

    //players
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    @JsonManagedReference("team-players")
    private List<Player> players = new ArrayList<>();

    // Team.java
    @OneToOne(mappedBy = "team", cascade = CascadeType.ALL)
    private PtsTable pointsTableEntry;
    private String status;

    @PrePersist
    public void prePersist() {
        if (this.status == null || this.status.isBlank()) {
            this.status = "DRAFT";
        } else {
            this.status = this.status.toUpperCase();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (this.status != null) {
            this.status = this.status.toUpperCase();
        }
    }
}
