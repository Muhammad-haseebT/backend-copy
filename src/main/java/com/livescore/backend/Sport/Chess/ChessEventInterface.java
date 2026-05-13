package com.livescore.backend.Sport.Chess;

import com.livescore.backend.Entity.Chess.ChessEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChessEventInterface extends JpaRepository<ChessEvent, Long> {

    Optional<ChessEvent> findTopByMatch_IdOrderByIdDesc(Long matchId);

    @Query("""
        SELECT e FROM ChessEvent e
        LEFT JOIN FETCH e.player
        LEFT JOIN FETCH e.team
        WHERE e.match.id = :matchId
        ORDER BY e.id ASC
    """)
    List<ChessEvent> findByMatch_IdOrderByIdAsc(@Param("matchId") Long matchId);

    @Query("""
        SELECT e FROM ChessEvent e
        WHERE e.match.tournament.id = :tournamentId
        AND e.player.id = :playerId
        ORDER BY e.id ASC
    """)
    List<ChessEvent> findByPlayerIdAndTournamentId(
            @Param("playerId") Long playerId,
            @Param("tournamentId") Long tournamentId
    );

    @Query("""
        SELECT COUNT(e) FROM ChessEvent e
        WHERE e.match.id = :matchId
        AND e.eventType = 'MOVE'
        AND e.team.id = :teamId
    """)
    int countMovesByMatchAndTeam(@Param("matchId") Long matchId, @Param("teamId") Long teamId);
}
