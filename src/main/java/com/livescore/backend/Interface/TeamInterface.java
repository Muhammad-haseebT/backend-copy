package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInterface extends JpaRepository<Team, Long> {


    @Query("SELECT t FROM Team t WHERE t.status = 'APPROVED' and t.tournament.id=:id")
    List<Team> findByTournamentId(@Param("id") Long tournamentId);

    List<Team> findByTournamentIdAndStatus(Long tournamentId, String status);

    List<Team> findByStatus(String status);

    @Query("SELECT t FROM Team t WHERE t.creator.id = :aid AND t.tournament.id = :tid")
    Optional<Team> findByTournamentIdAndPlayerId(@Param("tid") Long tid,
                                                 @Param("aid") Long aid);


    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM Team t JOIN t.players p " +
            "WHERE  p.id = :playerId AND t.tournament.id = :tournamentId")
    boolean findBytournamentAndPlayerAndTeam(@Param("tournamentId") Long tournamentId,
                                             @Param("playerId") Long playerId);

    @Query("""
            select distinct p.id
            from Team t join t.players p
            where t.tournament.id = :tournamentId
            """)
    List<Long> findTournamentPlayerIds(@Param("tournamentId") Long tournamentId);

    @Query("select p from Team t join t.players p where t.id = :id")
    List<Player> findPlayersByteamId(Long id);
}
