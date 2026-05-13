package com.livescore.backend.Sport.Futsal;

import com.livescore.backend.Entity.Futsal.FutsalMatchStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FutsalMatchStatsInterface extends JpaRepository<FutsalMatchStats, Long> {

    // Single player, single match
    Optional<FutsalMatchStats> findByMatch_IdAndPlayer_Id(Long matchId, Long playerId);

    // All players in a match (for match summary)
    List<FutsalMatchStats> findByMatch_Id(Long matchId);

    // All matches for a player (for progress tracking)
    List<FutsalMatchStats> findByPlayer_IdOrderByMatch_IdAsc(Long playerId);

    // All matches for a player in a tournament
    @Query("""
        SELECT ms FROM FutsalMatchStats ms
        WHERE ms.player.id = :playerId
        AND ms.match.tournament.id = :tournamentId
        ORDER BY ms.match.id ASC
    """)
    List<FutsalMatchStats> findByPlayerAndTournament(
            @Param("playerId") Long playerId,
            @Param("tournamentId") Long tournamentId
    );

    // All players from a team in a specific match
    List<FutsalMatchStats> findByMatch_IdAndTeam_Id(Long matchId, Long teamId);
}