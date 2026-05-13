package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.PlayerRequest;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRequestInterface extends JpaRepository<PlayerRequest,Long> {
        @Query("SELECT pr.player FROM PlayerRequest pr WHERE pr.team.id = :teamId AND pr.status = 'APPROVED' AND pr.player.isDeleted = false")
        List<Player> findApprovedPlayersByTeamId(@Param("teamId") Long teamId);
    @Query("""
SELECT pr
FROM PlayerRequest pr
WHERE pr.player.id = :playerId
AND pr.tournament.id = :tournamentId
AND pr.status = 'APPROVED'
AND pr.player.isDeleted = false
""")
    PlayerRequest findExistingRequest(Long playerId, Long tournamentId);


    @Query("SELECT pr FROM PlayerRequest pr WHERE pr.player.id = :playerId AND pr.player.isDeleted = false and pr.status='PENDING'")
    List<PlayerRequest> findbyPlayer_Id(@Param("playerId") Long playerId);

    Optional<PlayerRequest> findByPlayer_IdAndTeam_Id(Long playerId, Long teamId);


    List<PlayerRequest> findByTeam_Id(Long id);

    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END " +
            "FROM PlayerRequest pr " +
            "WHERE pr.player = :player AND pr.tournament = :tournament AND pr.team=:team")
    boolean existsByPlayerAndTournament(Player player, Tournament tournament, Team team);
    @Query("SELECT pr FROM PlayerRequest pr " +
            "LEFT JOIN FETCH pr.player p " +
            "LEFT JOIN FETCH p.account " +
            "WHERE pr.tournament.id = :tournamentId")
    List<PlayerRequest> findByTournamentIdWithPlayerAndAccount(@Param("tournamentId") Long tournamentId);
    @Query("SELECT pr FROM PlayerRequest pr " +
            "LEFT JOIN FETCH pr.player p " +
            "WHERE pr.team.id = :teamId")
    List<PlayerRequest> findByTeamIdWithPlayer(@Param("teamId") Long teamId);
}





