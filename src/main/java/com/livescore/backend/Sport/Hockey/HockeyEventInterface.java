package com.livescore.backend.Sport.Hockey;

import com.livescore.backend.Entity.Hockey.HockeyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HockeyEventInterface extends JpaRepository<HockeyEvent, Long> {

    // Last event for undo
    Optional<HockeyEvent> findTopByMatch_IdOrderByIdDesc(Long matchId);

    // All events for a match, in order
    List<HockeyEvent> findByMatch_IdOrderByIdAsc(Long matchId);

    // Events for a player in a tournament (for tournament-level stats recalculation)
    @Query("SELECT e FROM HockeyEvent e WHERE e.player.id = :playerId AND e.match.tournament.id = :tournamentId")
    List<HockeyEvent> findByPlayerIdAndTournamentId(Long playerId, Long tournamentId);
}
