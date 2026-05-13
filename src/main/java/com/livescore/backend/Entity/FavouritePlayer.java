package com.livescore.backend.Entity;

import com.livescore.backend.Config.FeedbackMapConverter;
import com.livescore.backend.Config.MapConverter;
import com.livescore.backend.Config.SetConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.util.*;

@Entity
@Data
@Table(name = "favourite_player")
public class FavouritePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id", unique = true)
    private Match match;

    // [101, 102, 103]
    @Convert(converter = SetConverter.class)
    @Column(columnDefinition = "TEXT")
    private Set<Long> votedAccountIds = new HashSet<>();

    // {"12": 45, "7": 30}
    @Convert(converter = MapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<Long, Integer> playerVoteCounts = new HashMap<>();

    @Convert(converter = FeedbackMapConverter.class)
    @Column(name = "feedbacks", columnDefinition = "TEXT")
    private Map<Long, String> feedbacks = new HashMap<>();
}
