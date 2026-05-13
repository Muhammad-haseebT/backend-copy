package com.livescore.backend.Sport.Ludo;

import com.livescore.backend.Entity.Ludo.LudoEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LudoEventInterface extends JpaRepository<LudoEvent, Long> {

    Optional<LudoEvent> findTopByMatch_IdOrderByIdDesc(Long matchId);

    @Query("""
        SELECT e FROM LudoEvent e
        LEFT JOIN FETCH e.player
        LEFT JOIN FETCH e.team
        WHERE e.match.id = :matchId
        ORDER BY e.id ASC
    """)
    List<LudoEvent> findByMatch_IdOrderByIdAsc(@Param("matchId") Long matchId);

    @Query("""
        SELECT e FROM LudoEvent e
        WHERE e.match.tournament.id = :tournamentId
        AND e.player.id = :playerId
        ORDER BY e.id ASC
    """)
    List<LudoEvent> findByPlayerIdAndTournamentId(
            @Param("playerId") Long playerId,
            @Param("tournamentId") Long tournamentId
    );
}
