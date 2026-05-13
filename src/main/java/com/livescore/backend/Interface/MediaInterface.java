package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Media;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaInterface extends JpaRepository<Media,Long> {

    @Query("SELECT m FROM Media m WHERE m.match.id = :matchId")
    List<Media> findByMatchId(@Param("matchId") Long ID);
    @Query("""
    SELECT m from Media m join  m.match ma join m.match.tournament t join t.season s where s.id = :seasonId
""")
    List<Media> findMediaBySeasonId(@Param("seasonId") Long seasonId, Pageable pageable);

    @Query("SELECT m FROM Media m JOIN m.match ma WHERE ma.tournament.id = :id")
    List<Media> findMediaByTournamentId(Long id, Pageable pageable);

    @Query("SELECT m FROM Media m JOIN m.match ma WHERE ma.tournament.sport.id = :id")
    List<Media> findMediaBySportId(Long id, Pageable pageable);

    @Query("select m from  Media m left join m.ball b where b.id=:id")
    List<Media> findByBallId(Long id);
}
