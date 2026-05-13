package com.livescore.backend.Sport.Badminton;

import com.livescore.backend.Entity.Badminton.BadmintonEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BadmintonEventInterface extends JpaRepository<BadmintonEvent, Long> {

    Optional<BadmintonEvent> findTopByMatch_IdOrderByIdDesc(Long matchId);

    @Query("""
        SELECT e FROM BadmintonEvent e
        LEFT JOIN FETCH e.player
        LEFT JOIN FETCH e.inPlayer
        LEFT JOIN FETCH e.team
        WHERE e.match.id = :matchId
        ORDER BY e.id ASC
    """)
    List<BadmintonEvent> findByMatch_IdOrderByIdAsc(@Param("matchId") Long matchId);

    @Query("""
        SELECT e FROM BadmintonEvent e
        WHERE e.match.tournament.id = :tournamentId
        AND e.player.id = :playerId
        ORDER BY e.id ASC
    """)
    List<BadmintonEvent> findByPlayerIdAndTournamentId(
            @Param("playerId") Long playerId,
            @Param("tournamentId") Long tournamentId
    );
}
