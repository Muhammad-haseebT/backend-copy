package com.livescore.backend.Interface;

import com.livescore.backend.Entity.CricketInnings;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Stats;
import com.livescore.backend.Entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchInterface extends JpaRepository<Match,Long> {

    @Query("SELECT m FROM Match m WHERE m.tournament.id = :tournamentId")
    List<Match> findByTournamentId(@Param("tournamentId") Long tournamentId);
@Query("select m from Match m where (m.team1.id = :teamId or m.team2.id = :teamId) and m.tournament.id = :tournamentId and m.status = 'COMPLETED'")
    List<Match> findCompletedMatchesByTeam(Long teamId, Long tournamentId);



    List<Match> findByStatus(String status);



    List<Match> findByDate(LocalDate date);

    @Query("SELECT m FROM Match m WHERE m.time = :time")
    List<Match> findByTime(@Param("time") LocalTime time);



    @Query("SELECT m FROM Match m WHERE m.team1.id = :team1 OR m.team2.id = :team2")
    List<Match> findByTeam1IdOrTeam2Id(@Param("team1") Long team1, @Param("team2") Long team2);


    @Query("SELECT m.manOfMatch.id, COUNT(m) FROM Match m WHERE m.tournament.id = :tournamentId AND m.manOfMatch IS NOT NULL GROUP BY m.manOfMatch.id ORDER BY COUNT(m) DESC")
    List<Object[]> countPlayerOfMatchByPlayer(@Param("tournamentId") Long tournamentId);

    @Query("SELECT COUNT(m) FROM Match m WHERE m.tournament.id = :tournamentId AND m.manOfMatch.id = :playerId")
    long countManOfMatchForPlayer(@Param("tournamentId") Long tournamentId, @Param("playerId") Long playerId);

    @Query("SELECT COUNT(m) FROM Match m WHERE m.manOfMatch.id = :playerId")
    long countManOfMatchForPlayerOverall(@Param("playerId") Long playerId);

    List<Match> findByTournament_Id(Long tournamentId);


    @Query("SELECT m FROM Match m WHERE m.tournament.sport.name = :name AND m.status = :status")
    List<Match> findByTournament_SportName(@Param("name") String name,@Param("status") String status);

    @Query("SELECT m FROM Match m WHERE m.tournament.sport.name = :name")
    List<Match> findByTournament_SportName(@Param("name") String name);

    @Query("Select m from Match m where (m.scorer.id=:id or m.mediaScorer.id=:id) and m.status in ('LIVE','UPCOMING')")
    List<Match> findByScorerID(@Param("id") Long id);

    @Query("""
    SELECT COUNT(m)
    FROM Match m
    WHERE EXISTS (
        SELECT p FROM m.team1.players p WHERE p.id = :playerId
    )
    OR EXISTS (
        SELECT p FROM m.team2.players p WHERE p.id = :playerId
    )
""")int findMatchesByTeam(@Param("playerId") Long playerId);

@Query("select  count(*) from Match m where m.manOfMatch.id=:playerId")
    int findMatchesBPom(Long playerId);

    @Query("""
    SELECT m FROM Match m
    JOIN FETCH m.tournament t
    JOIN FETCH t.sport
    WHERE m.id = :id
""")
    Optional<Match> findByIdWithSport(@Param("id") Long id);
//

    @Query(value = """
    SELECT COUNT(DISTINCT m.id) 
    FROM match m
    JOIN tournament t ON m.tournament_id = t.id
    JOIN sports s ON t.sports_id = s.id
    JOIN stats st ON st.tournament_id = t.id
    WHERE m.status = 'COMPLETED'
    AND s.name = 'Cricket'
    AND st.player_id = ?1
""", nativeQuery = true)
    int findCricketMatchesByPlayer(Long playerId);

    @Query(value = """
    SELECT COUNT(DISTINCT m.id) 
    FROM match m
    JOIN tournament t ON m.tournament_id = t.id
    JOIN sports s ON t.sports_id = s.id
    JOIN stats st ON st.tournament_id = t.id
    WHERE m.status = 'COMPLETED'
    AND s.name = 'Futsal'
       AND LOWER(s.name) LIKE LOWER('%futsal%')
    AND st.player_id = ?1
""", nativeQuery = true)
    int findFutsalMatchesByPlayer(Long playerId);

    @Query(value = """
    SELECT COUNT(DISTINCT m.id) 
    FROM match m
    JOIN tournament t ON m.tournament_id = t.id
    JOIN sports s ON t.sports_id = s.id
    JOIN stats st ON st.tournament_id = t.id
    WHERE m.status = 'COMPLETED'
    AND s.name = 'VolleyBall'
       AND LOWER(s.name) LIKE LOWER('%volley%ball%')
    AND st.player_id = ?1
""", nativeQuery = true)
    int findVolleyballMatchesByPlayer(Long playerId);

    @Query(value = """
    SELECT COUNT(DISTINCT m.id) 
    FROM match m
    JOIN tournament t ON m.tournament_id = t.id
    JOIN sports s ON t.sports_id = s.id
    JOIN stats st ON st.tournament_id = t.id
    WHERE m.status = 'COMPLETED'
    AND s.name = 'Badminton'
    AND LOWER(s.name) LIKE LOWER('%badminton%')
    AND st.player_id = ?1
""", nativeQuery = true)
    int findBadmintonMatchesByPlayer(Long playerId);

    @Query(value = """
    SELECT COUNT(DISTINCT m.id)\s
    FROM match m
    JOIN tournament t ON m.tournament_id = t.id
    JOIN sports s ON t.sports_id = s.id
    JOIN stats st ON st.tournament_id = t.id
    WHERE LOWER(m.status) = LOWER('COMPLETED')
    AND LOWER(s.name) LIKE LOWER('%table%tennis%')
    AND st.player_id = ?1
""", nativeQuery = true)
    int findTableTennisMatchesByPlayer(Long playerId);

    @Query(value = """
    SELECT COUNT(DISTINCT m.id) 
    FROM match m
    JOIN tournament t ON m.tournament_id = t.id
    JOIN sports s ON t.sports_id = s.id
    JOIN stats st ON st.tournament_id = t.id
    WHERE LOWER(m.status) = LOWER('COMPLETED')
    AND (LOWER(s.name) LIKE LOWER('%tug%war%') OR REPLACE(LOWER(s.name), ' ', '') = 'tugofwar')
    AND st.player_id = ?1
""", nativeQuery = true)
    int findTableTugOfWarMatchesByPlayer(Long playerId);
    @Query(value = """
    SELECT COUNT(DISTINCT m.id) 
    FROM match m
    JOIN tournament t ON m.tournament_id = t.id
    JOIN sports s ON t.sports_id = s.id
    JOIN stats st ON st.tournament_id = t.id
    WHERE LOWER(m.status) = LOWER('COMPLETED')
    AND (LOWER(s.name) LIKE LOWER('%ludo%') OR REPLACE(LOWER(s.name), ' ', '') = 'ludo')
    AND st.player_id = ?1
""", nativeQuery = true)
    int findLudoMatchesByPlayer(Long playerId);
    @Query(value = """
    SELECT COUNT(DISTINCT m.id) 
    FROM match m
    JOIN tournament t ON m.tournament_id = t.id
    JOIN sports s ON t.sports_id = s.id
    JOIN stats st ON st.tournament_id = t.id
    WHERE LOWER(m.status) = LOWER('COMPLETED')
    AND (LOWER(s.name) LIKE LOWER('%chess%') OR REPLACE(LOWER(s.name), ' ', '') = 'chess')
    AND st.player_id = ?1
""", nativeQuery = true)
    int findChessMatchesByPlayer(Long playerId);



    @Query("select m.cricketInnings from Match m where m.id=:id")
    List<CricketInnings> findteambyinningsandmatch(Long id);
}
