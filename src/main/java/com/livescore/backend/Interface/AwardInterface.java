package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Award;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Stats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AwardInterface extends JpaRepository<Award, Long> {

    List<Award> findByMatchId(Long matchId);

    List<Award> findByTournamentId(Long tournamentId);

    List<Award> findByPlayerId(Long playerId);

    @Query("SELECT a FROM Award a WHERE a.match.id = :matchId AND a.awardType = :type ORDER BY a.id ASC")
    List<Award> findByMatchIdAndAwardType(@Param("matchId") Long matchId, @Param("type") String type);

    @Query("SELECT a FROM Award a WHERE a.tournament.id = :tournamentId AND a.awardType = :type")
    Optional<Award> findByTournamentIdAndAwardType(@Param("tournamentId") Long tournamentId,
                                                   @Param("type") String type);

    @Query("SELECT COUNT(a) FROM Award a WHERE a.player.id = :playerId AND a.awardType = 'PLAYER_OF_MATCH'")
    int countPomByPlayerId(@Param("playerId") Long playerId);



    // ---- AwardInterface ----
// ADD:
    boolean existsByTournamentIdAndAwardType(Long tournamentId, String awardType);

    @Query("""
    SELECT COUNT(a) FROM Award a
    WHERE a.player.id = :playerId
    AND a.awardType = 'PLAYER_OF_MATCH'
    AND LOWER(a.match.tournament.sport.name) = LOWER(:sport)
""")
    int countPomByPlayerIdAndSport(
            @Param("playerId") Long playerId,
            @Param("sport") String sport
    );
    @Query("SELECT a FROM Award a WHERE a.tournament.id = :tournamentId AND a.awardType = :type ORDER BY a.id ASC")
    List<Award> findAllByTournamentIdAndAwardType(@Param("tournamentId") Long tournamentId,
                                                  @Param("type") String type);
}