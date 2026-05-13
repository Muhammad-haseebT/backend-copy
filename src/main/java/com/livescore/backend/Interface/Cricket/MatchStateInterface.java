package com.livescore.backend.Interface.Cricket;

import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.MatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MatchStateInterface extends JpaRepository<MatchState, Long> {
    MatchState findByInnings_Id(Long inningsId);

    Long innings(CricketInnings innings);

    @Query("SELECT ms FROM MatchState ms WHERE ms.innings.team.id = :teamId AND ms.innings.match.id = :matchId AND ms.innings.isSuper_Over = false")
    MatchState findByTeam_Id(Long teamId, Long matchId);

    @Query("select  m from MatchState m where m.innings.no=:i and m.innings.match.id=:mid")
    MatchState findByMatchIdAndInningsNo(Long mid, int i);
}
