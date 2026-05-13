package com.livescore.backend.Sport.TableTennis;

import com.livescore.backend.Entity.TableTennis.TableTennisEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TableTennisEventInterface extends JpaRepository<TableTennisEvent, Long> {

    Optional<TableTennisEvent> findTopByMatch_IdOrderByIdDesc(Long matchId);

    @Query("""
        SELECT e FROM TableTennisEvent e
        LEFT JOIN FETCH e.player
        LEFT JOIN FETCH e.team
        WHERE e.match.id = :matchId
        ORDER BY e.id ASC
    """)
    List<TableTennisEvent> findByMatch_IdOrderByIdAsc(@Param("matchId") Long matchId);

    @Query("""
        SELECT e FROM TableTennisEvent e
        WHERE e.match.tournament.id = :tournamentId
        AND e.player.id = :playerId
        ORDER BY e.id ASC
    """)
    List<TableTennisEvent> findByPlayerIdAndTournamentId(
            @Param("playerId") Long playerId,
            @Param("tournamentId") Long tournamentId
    );
}
