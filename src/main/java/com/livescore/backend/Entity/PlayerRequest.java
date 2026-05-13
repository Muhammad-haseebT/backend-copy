package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Entity
@Getter
@Setter

public class PlayerRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PlayerTeam -> Player (many-to-one)
    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    @JsonBackReference("player-playerTeams")
    private Player player;

    // PlayerTeam -> Team (many-to-one)
    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    @JsonBackReference("team-playerTeams")
    private Team team;

    // PlayerTeam -> Tournament (many-to-one)
    @ManyToOne
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonIgnore // avoid nesting tournament -> teams -> playerTeams cycles
    private Tournament tournament;

    private String status;

    @PrePersist
    public void prePersist() {
        if (this.status == null || this.status.isBlank()) {
            this.status = "PENDING";
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
