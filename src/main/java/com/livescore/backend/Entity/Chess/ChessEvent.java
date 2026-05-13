package com.livescore.backend.Entity.Chess;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Team;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "chess_events")
public class ChessEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    // Team making the move / event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    // ── Event types ───────────────────────────────────────────────
    // MOVE          — team made a move (increments move count + switches turn)
    // CHECK         — team delivered a check to opponent
    // CHECKMATE     — team wins by checkmate
    // STALEMATE     — draw by stalemate
    // RESIGN        — team resigns (opponent wins)
    // TIMEOUT       — team ran out of time (opponent wins)
    // DRAW_AGREED   — both teams agreed to draw
    // END_MATCH     — manual match end
    @Column(name = "event_type", nullable = false)
    private String eventType;

    // Move notation e.g. "e4", "Nf3", "O-O"
    @Column(name = "move_notation")
    private String moveNotation;

    @Column(name = "move_number")
    private Integer moveNumber;

    @Column(name = "event_time_seconds")
    private Integer eventTimeSeconds;
}
