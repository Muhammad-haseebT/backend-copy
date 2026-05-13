package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Entity.TeamRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRequestInterface extends JpaRepository<TeamRequest, Long> {


    boolean existsByTeamAndPlayerAccount(Team team, Account player);

    @Query("SELECT tr FROM TeamRequest tr WHERE tr.team.tournament.id = :tournamentId AND tr.status = 'PENDING'")
    List<TeamRequest> findByTournamentId(Long tournamentId);

    List<TeamRequest> findAllByStatus(String status);
}
