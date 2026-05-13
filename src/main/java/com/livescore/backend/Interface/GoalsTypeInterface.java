package com.livescore.backend.Interface;

import com.livescore.backend.Entity.GoalsType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoalsTypeInterface extends JpaRepository<GoalsType,Long> {

    @Query("""
        SELECT gt FROM GoalsType gt
        WHERE gt.match.tournament.id = :tournamentId
          AND gt.player.id = :playerId
          AND gt.player.isDeleted = false
    """)
    List<GoalsType> findGoalsByTournamentAndPlayer(@Param("tournamentId") Long tournamentId,
                                                   @Param("playerId") Long playerId);

    // optionally for match-limited update:
    @Query("""
        SELECT gt FROM GoalsType gt
        WHERE gt.match.id = :matchId
          AND gt.player.id = :playerId
          AND gt.player.isDeleted = false
    """)
    List<GoalsType> findGoalsByMatchAndPlayer(@Param("matchId") Long matchId,
                                              @Param("playerId") Long playerId);
}
