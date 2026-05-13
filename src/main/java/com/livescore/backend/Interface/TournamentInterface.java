package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface TournamentInterface extends JpaRepository<Tournament,Long> {
    boolean existsByNameAndSeasonId(String name, Long seasonId);

    @Query("""
            SELECT t
            FROM Tournament t
            WHERE t.season.id = :seasonId AND t.sport.id = :sportId
            """)
    List<Tournament> findBySeasonIdAndSportName(@Param("seasonId") Long seasonId, @Param("sportId") Long sportId);

    @Query("select t from Tournament t")
    List<Tournament> findAllNames();
}
