package com.livescore.backend.Sport.Volleyball;

import com.livescore.backend.Entity.Volleyball.VolleyballEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface VolleyballEventInterface extends JpaRepository<VolleyballEvent, Long> {

    Optional<VolleyballEvent> findTopByMatch_IdOrderByIdDesc(Long matchId);

    List<VolleyballEvent> findByMatch_IdOrderByIdAsc(Long matchId);

    @Query("""
        SELECT e FROM VolleyballEvent e
        WHERE e.match.tournament.id = :tournamentId
        AND e.player.id = :playerId
        ORDER BY e.id ASC
    """)
    List<VolleyballEvent> findByPlayerIdAndTournamentId(
            @Param("playerId") Long playerId,
            @Param("tournamentId") Long tournamentId
    );


    @Query("""
    SELECT e FROM VolleyballEvent e
    LEFT JOIN FETCH e.player
    LEFT JOIN FETCH e.inPlayer
    LEFT JOIN FETCH e.team
    WHERE e.match.id = :matchId
    ORDER BY e.id ASC
""")
    List<VolleyballEvent> findByMatch_IdOrderByIdAscWithPlayers(
            @Param("matchId") Long matchId
    );
}
