package com.livescore.backend.Interface.Cricket;

import com.livescore.backend.Entity.PlayerInnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerInningsInterface extends JpaRepository<PlayerInnings, Long> {
    PlayerInnings findByInnings_IdAndPlayer_Id(Long inningsId, Long bowlerId);


    @Query("SELECT pi FROM PlayerInnings pi WHERE pi.innings.match.id = :matchId and pi.innings.team.id=:teamId")
    List<PlayerInnings> findByMatchIdAndTeamId(Long matchId, Long teamId);

    @Query("SELECT pi from PlayerInnings pi where pi.innings.match.id = :mid and pi.innings.no=:no")
    List<PlayerInnings> findByMatchIdAndInninsNo(Long mid,int no);
    List<PlayerInnings> findByInnings_Id(Long inningsId);
}
