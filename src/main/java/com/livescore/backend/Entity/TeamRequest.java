package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// TeamRequest.java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TeamRequest -> Team
    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    @JsonBackReference("team-requests")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "player_account_id", nullable = false)
    @JsonBackReference("account-requests")
    private Account playerAccount;

    @Column(nullable = false)
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
