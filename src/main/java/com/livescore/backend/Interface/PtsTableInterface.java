package com.livescore.backend.Interface;

import com.livescore.backend.Entity.PtsTable;
import com.livescore.backend.Service.Abc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PtsTableInterface extends JpaRepository<PtsTable,Long> {

    boolean existsByTeamIdAndTournamentId(Long id, Long id1);


    @Query("SELECT pt FROM PtsTable pt WHERE pt.tournament.id = :tournamentId AND pt.team.id = :teamId")

    PtsTable findByTournamentIdAndTeamId(@Param("tournamentId") Long tournamentId, @Param("teamId") Long teamId);


    @Query("SELECT new com.livescore.backend.Service.Abc(pt.team.name,pt.points) FROM PtsTable pt WHERE pt.tournament.id = :tournamentId ORDER BY pt.points  Desc")
     List<Abc> findByTournamentId(@Param("tournamentId") Long tournamentId, Pageable pageable);


    List<PtsTable> findByTournamentId(Long tournamentId);


    PtsTable findByTeamIdAndTournamentId(Long id, Long id1);
}
