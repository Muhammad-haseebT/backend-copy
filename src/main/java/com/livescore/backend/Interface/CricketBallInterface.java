package com.livescore.backend.Interface;

import com.livescore.backend.DTO.CricketBallsScoringDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Stats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface CricketBallInterface extends JpaRepository<CricketBall, Long> {



    // ✅ MISSING METHOD - Add this for ball auto-increment
    @Query("SELECT cb FROM CricketBall cb WHERE cb.innings.id = :inningsId ORDER BY cb.id DESC LIMIT 1")
    CricketBall findLastBallInInnings(@Param("inningsId") Long inningsId);

    @Query("""
        SELECT cb FROM CricketBall cb
        WHERE cb.batsman.id = :playerId
          AND cb.innings.match.tournament.id = :tournamentId
    """)
    List<CricketBall> findBatsmanBalls(@Param("tournamentId") Long tournamentId,
                                       @Param("playerId") Long playerId);

    @Query("""
        SELECT cb FROM CricketBall cb
        WHERE cb.bowler.id = :playerId
          AND cb.innings.match.tournament.id = :tournamentId
    """)
    List<CricketBall> findBowlerBalls(@Param("tournamentId") Long tournamentId,
                                      @Param("playerId") Long playerId);

    @Query("""
        SELECT cb FROM CricketBall cb
        WHERE cb.fielder.id = :playerId
          AND cb.innings.match.tournament.id = :tournamentId
    """)
    List<CricketBall> findFielderBalls(@Param("tournamentId") Long tournamentId,
                                       @Param("playerId") Long playerId);

    List<CricketBall> findByMatch_Id(Long matchId);


    @Query("SELECT COUNT(b) FROM CricketBall b WHERE b.innings.id = :inningsId AND b.legalDelivery = true")
    long countLegalBallsByInningsId(@Param("inningsId") Long inningsId);

    @Query("""
      SELECT COALESCE(SUM(COALESCE(b.runs,0) + COALESCE(b.extra,0)), 0)
      FROM CricketBall b
      WHERE b.innings.id = :inningsId
    """)
    int sumRunsAndExtrasByInningsId(@Param("inningsId") Long inningsId);

    @Query("""
      SELECT COUNT(b)
      FROM CricketBall b
      WHERE b.innings.id = :inningsId
        AND b.dismissalType IS NOT NULL
        AND LOWER(b.dismissalType) NOT IN ('retired hurt', 'retired')
    """)
    long countWicketsByInningsId(@Param("inningsId") Long inningsId);



    @Query("SELECT cb FROM CricketBall cb WHERE cb.overNumber = :overNumber AND cb.ballNumber = :ballNumber AND cb.match.id = :matchId AND cb.innings.no = :inningsNo")
    List<CricketBall> findByOverNumberAndBallNumberAndMatch_Id(@Param("overNumber") Integer overNumber, @Param("ballNumber") Integer ballNumber, @Param("matchId") Long matchId, @Param("inningsNo") int inningsNo);



    @Query("SELECT COUNT(DISTINCT cb.innings.id) FROM CricketBall cb WHERE cb.batsman.id = :playerId AND cb.match.tournament.id = :tournamentId")
    int countDistinctInningsBatted(@Param("playerId") Long playerId, @Param("tournamentId") Long tournamentId);



    @Query("""
SELECT COUNT(DISTINCT b.match.id)
FROM CricketBall b
WHERE (b.batsman.id = :playerId 
       OR b.bowler.id = :playerId 
       OR b.fielder.id = :playerId)
  AND b.match.tournament.id = :tournamentId
""")
    int countMatchesPlayedInTournament(Long playerId, Long tournamentId);

    // Optimized batting aggregate
    @Query("""
SELECT SUM(cb.runs) as totalRuns,
       SUM(CASE WHEN LOWER(cb.extraType) LIKE '%wide%' THEN 0 ELSE 1 END) as ballsFaced,
       SUM(CASE WHEN cb.isFour = true THEN 1 ELSE 0 END) as fours,
       SUM(CASE WHEN cb.isSix = true THEN 1 ELSE 0 END) as sixes
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
  AND cb.match.tournament.id = :tournamentId
""")
    Object[] getBattingAggregate(@Param("playerId") Long playerId,
                                 @Param("tournamentId") Long tournamentId);

    // Optimized bowling aggregate
    @Query("""
SELECT SUM(cb.runs + CASE WHEN LOWER(cb.extraType) LIKE '%wide%' OR LOWER(cb.extraType) LIKE '%no%' THEN cb.extra ELSE 0 END) as runsConceded,
       SUM(CASE WHEN cb.legalDelivery = true THEN 1 ELSE 0 END) as ballsBowled,
       SUM(CASE WHEN cb.dismissalType IS NOT NULL 
                AND LOWER(cb.dismissalType) IN ('bowled','lbw','stumped','caught','hit wicket','hitwicket')
           THEN 1 ELSE 0 END) as wickets
FROM CricketBall cb
WHERE cb.bowler.id = :playerId
  AND cb.match.tournament.id = :tournamentId
""")
    Object[] getBowlingAggregate(@Param("playerId") Long playerId,
                                 @Param("tournamentId") Long tournamentId);

    @Query("""
SELECT COUNT(DISTINCT cb.innings.id) 
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
  AND cb.match.tournament.id = :tournamentId
  AND cb.innings.id NOT IN (
    SELECT DISTINCT cb2.innings.id
    FROM CricketBall cb2
    WHERE cb2.batsman.id = :playerId
      AND cb2.match.tournament.id = :tournamentId
      AND cb2.dismissalType IS NOT NULL
  )
""")
    Integer countNotOutInnings(@Param("playerId") Long playerId,
                               @Param("tournamentId") Long tournamentId);

    @Query("""
SELECT SUM(cb.runs)
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
  AND cb.match.tournament.id = :tournamentId
GROUP BY cb.innings.id
ORDER BY SUM(cb.runs) DESC
""")
    List<Integer> getRunsPerInningsDesc(@Param("playerId") Long playerId,
                                        @Param("tournamentId") Long tournamentId,
                                        Pageable pageable);

    // Overall stats (no tournament filter)
    @Query("""
SELECT SUM(cb.runs) as totalRuns,
       SUM(CASE WHEN LOWER(cb.extraType) LIKE '%wide%' THEN 0 ELSE 1 END) as ballsFaced,
       SUM(CASE WHEN cb.isFour = true THEN 1 ELSE 0 END) as fours,
       SUM(CASE WHEN cb.isSix = true THEN 1 ELSE 0 END) as sixes
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
""")
    Object[] getBattingAggregateOverall(@Param("playerId") Long playerId);

    @Query("""
SELECT SUM(cb.runs + CASE WHEN LOWER(cb.extraType) LIKE '%wide%' OR LOWER(cb.extraType) LIKE '%no%' THEN cb.extra ELSE 0 END) as runsConceded,
       SUM(CASE WHEN cb.legalDelivery = true THEN 1 ELSE 0 END) as ballsBowled,
       SUM(CASE WHEN cb.dismissalType IS NOT NULL
                AND LOWER(cb.dismissalType) IN ('bowled','lbw','stumped','caught','hit wicket','hitwicket')
           THEN 1 ELSE 0 END) as wickets
FROM CricketBall cb
WHERE cb.bowler.id = :playerId
""")
    Object[] getBowlingAggregateOverall(@Param("playerId") Long playerId);

    @Query("""
SELECT COUNT(DISTINCT cb.innings.id)
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
  AND cb.innings.id NOT IN (
    SELECT DISTINCT cb2.innings.id
    FROM CricketBall cb2
    WHERE cb2.batsman.id = :playerId
      AND cb2.dismissalType IS NOT NULL
  )
""")
    Integer countNotOutInningsOverall(@Param("playerId") Long playerId);

    @Query("""
SELECT SUM(cb.runs)
FROM CricketBall cb
WHERE cb.batsman.id = :playerId
GROUP BY cb.innings.id
ORDER BY SUM(cb.runs) DESC
""")
    List<Integer> getRunsPerInningsDescOverall(@Param("playerId") Long playerId,
                                               Pageable pageable);

    @Query("SELECT COUNT(DISTINCT cb.innings.id) FROM CricketBall cb WHERE cb.batsman.id = :playerId")
    int countDistinctInningsBattedOverall(@Param("playerId") Long playerId);

    @Query("""
SELECT COUNT(DISTINCT b.match.id)
FROM CricketBall b
WHERE (b.batsman.id = :playerId
       OR b.bowler.id = :playerId
       OR b.fielder.id = :playerId)
""")
    int countMatchesPlayedOverall(@Param("playerId") Long playerId);

    // Get all balls for an innings ordered by ID descending (for undo functionality)
    List<CricketBall> findByInnings_IdOrderByIdDesc(Long inningsId);

    @Query(value = "select new com.livescore.backend.DTO.CricketBallsScoringDTO(c.id,c.event,c.eventType,size(c.mediaList)) from CricketBall c where c.match.id=:MatchId and c.innings.id=:InningsId and c.super_over=:sp ")
    List<CricketBallsScoringDTO> getBalls(@Param("InningsId") Long inningsId,@Param("MatchId") Long matchId,@Param("sp") boolean sp);

    @Query("SELECT cb FROM CricketBall cb WHERE cb.innings.id = :inningsId AND cb.match.id = :matchId ORDER BY cb.id ASC")
    List<CricketBall> findAllByInningsAndMatch(@Param("inningsId") Long inningsId,
                                               @Param("matchId") Long matchId);

    @Query("""
    SELECT b FROM CricketBall b
    WHERE b.innings.match.tournament.id = :tournamentId
      AND (b.batsman.id = :playerId
           OR b.bowler.id  = :playerId
           OR b.fielder.id = :playerId)
    """)
    List<CricketBall> findAllByPlayerAndTournament(
            @Param("playerId") Long playerId,
            @Param("tournamentId") Long tournamentId);

    @Query("select cb from CricketBall cb where cb.innings.match.id=:mid and cb.innings.team.id=:tid and cb.innings.isSuper_Over = false order by cb.id asc")
    List<CricketBall> findByMatchId(Long mid, Long tid);


}
