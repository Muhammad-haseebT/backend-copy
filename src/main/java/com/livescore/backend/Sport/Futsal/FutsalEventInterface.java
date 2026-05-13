package com.livescore.backend.Sport.Futsal;

import com.livescore.backend.Entity.Futsal.FutsalEvent;
import com.livescore.backend.Entity.Futsal.FutsalMatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// Interface/Futsal/FutsalEventInterface.java
public interface FutsalEventInterface extends JpaRepository<FutsalEvent, Long> {

    // Last event dhundhne ke liye (undo ke liye)
    Optional<FutsalEvent> findTopByMatch_IdOrderByIdDesc(Long matchId);

    // Sare events ek match ke
    List<FutsalEvent> findByMatch_IdOrderByIdAsc(Long matchId);

    @Query("select e from FutsalEvent e where e.player.id = :playerId and e.match.tournament.id = :tournamentId")
    List<FutsalEvent> findByPlayerIdAndTournamentId(Long playerId, Long tournamentId);
}

