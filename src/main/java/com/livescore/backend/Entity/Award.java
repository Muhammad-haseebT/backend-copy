package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Award {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    @JsonIgnore
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    @JsonIgnore
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    @JsonIgnore
    private Player player;

    // "PLAYER_OF_MATCH", "MAN_OF_TOURNAMENT",
    // "BEST_BATSMAN", "BEST_BOWLER", "BEST_FIELDER"
    private String awardType;

    private Integer pointsEarned;
    private String reason;
    private LocalDateTime awardedAt;

    @PrePersist
    public void prePersist() {
        if (awardedAt == null) awardedAt = LocalDateTime.now();
    }
}