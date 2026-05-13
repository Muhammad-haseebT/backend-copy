package com.livescore.backend.Sport.TugOfWar;

import com.livescore.backend.Entity.TugOfWar.TugOfWarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TugOfWarEventInterface extends JpaRepository<TugOfWarEvent, Long> {

    Optional<TugOfWarEvent> findTopByMatch_IdOrderByIdDesc(Long matchId);

    @Query("""
        SELECT e FROM TugOfWarEvent e
        LEFT JOIN FETCH e.winnerTeam
        WHERE e.match.id = :matchId
        ORDER BY e.id ASC
    """)
    List<TugOfWarEvent> findByMatch_IdOrderByIdAsc(@Param("matchId") Long matchId);
}
