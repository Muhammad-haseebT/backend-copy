package com.livescore.backend.Interface;

import com.livescore.backend.Entity.CricketInnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CricketInningsInterface extends JpaRepository<CricketInnings, Long> {

    // Get all innings of a team inside a tournament
    @Query("SELECT ci FROM CricketInnings ci " +
            "WHERE ci.team.id = :teamId AND ci.match.tournament.id = :tournamentId")
    List<CricketInnings> findByTeamAndTournament(@Param("teamId") Long teamId, @Param("tournamentId") Long tournamentId);

    @Query("SELECT ci FROM CricketInnings ci WHERE ci.match.id = :matchId AND ci.team.id = :teamId ORDER BY ci.id ASC")
    List<CricketInnings> findByMatchAndTeamList(@Param("matchId") Long matchId, @Param("teamId") Long teamId);

    @Query("SELECT ci FROM CricketInnings ci WHERE ci.match.id = :matchId AND ci.team.id <> :teamId ORDER BY ci.id ASC")
    List<CricketInnings> findOpponentInningsList(@Param("matchId") Long matchId, @Param("teamId") Long teamId);
}
