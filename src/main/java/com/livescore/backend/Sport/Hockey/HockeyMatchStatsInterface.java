package com.livescore.backend.Sport.Hockey;

import com.livescore.backend.Entity.Hockey.HockeyMatchStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HockeyMatchStatsInterface extends JpaRepository<HockeyMatchStats, Long> {

    // Single player, single match
    Optional<HockeyMatchStats> findByMatch_IdAndPlayer_Id(Long matchId, Long playerId);

    // All players in a match (for match summary)
    List<HockeyMatchStats> findByMatch_Id(Long matchId);

    // All matches for a player in a tournament
    @Query("""
        SELECT ms FROM HockeyMatchStats ms
        WHERE ms.player.id = :playerId
        AND ms.match.tournament.id = :tournamentId
        ORDER BY ms.match.id ASC
    """)
    List<HockeyMatchStats> findByPlayerAndTournament(
            @Param("playerId") Long playerId,
            @Param("tournamentId") Long tournamentId
    );

    // All players from a team in a specific match
    List<HockeyMatchStats> findByMatch_IdAndTeam_Id(Long matchId, Long teamId);
}
