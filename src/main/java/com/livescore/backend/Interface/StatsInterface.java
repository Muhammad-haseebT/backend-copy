package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Stats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatsInterface extends JpaRepository<Stats,Long> {
    @Query("SELECT s FROM Stats s WHERE s.player.isDeleted = false")
    List<Stats> findAllActive();

    @Query("SELECT s FROM Stats s WHERE s.player.id = :playerId AND s.tournament.id = :tournamentId AND s.player.isDeleted = false")
    Optional<Stats> findByPlayerIdAndTournamentId(@Param("playerId") Long playerId, @Param("tournamentId") Long tournamentId);

    @Query("SELECT s FROM Stats s WHERE s.tournament.id = :tournamentId AND s.player.id = :playerId AND s.player.isDeleted = false")
    Optional<Stats> findByTournamentIdAndPlayerId(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);

    @Query("select s from Stats s where s.player.id=:playerId")
    List<Stats> findByPlayerId(@Param("playerId") Long playerId);

    List<Stats> findByTournamentId(Long tournamentId);


    // ---- StatsInterface ----
// ADD:
    List<Stats> findAllByTournamentId(Long tournamentId);



    List<Stats> findAllByPlayer_Id(Long playerId);

    // Sport-wise aggregate ke liye
    @Query("""
    SELECT s FROM Stats s
    WHERE s.player.id = :playerId
    AND LOWER(s.sportType.name) = LOWER(:sport)
""")
    List<Stats> findByPlayerIdAndSport(
            @Param("playerId") Long playerId,
            @Param("sport") String sport
    );


}
