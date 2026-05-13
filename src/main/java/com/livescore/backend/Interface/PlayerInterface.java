package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.PlayerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PlayerInterface extends JpaRepository<Player,Long> {

    @Query("SELECT p FROM Player p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Player> findById(@Param("id") Long id);

    @Query("SELECT (COUNT(p) > 0) FROM Player p WHERE p.id = :id AND p.isDeleted = false")
    boolean existsById(@Param("id") Long id);

    @Query("SELECT p FROM Player p WHERE p.isDeleted = false")
    List<Player> findAll();

    @Query("SELECT p FROM Player p JOIN p.playerRequests pr WHERE pr.team.id = :teamId AND pr.status = 'APPROVED' AND p.isDeleted = false")
    List<Player> findPlayersByTeamId(@Param("teamId") Long teamId);


    @Query("SELECT (COUNT(p) > 0) FROM Player p WHERE p.account.username = :username AND p.isDeleted = false")
    boolean existsByAccount_Username(@Param("username") String username);

    @Query("SELECT p FROM Player p WHERE p.id = :id AND p.isDeleted = false")
    Optional<Player> findActiveById(@Param("id") Long id);



    Optional<Player> findByAccount_Id(Long id);


    @Query("SELECT p FROM Player p WHERE p.account.id = :id AND p.isDeleted = false")
    Optional<Player> findActiveByAccount_Id(@Param("id") Long id);

    List<Player> findAllByAccount_Id(Long id);

    @Query("SELECT p FROM Player p WHERE p.id = :id")
    Optional<Player> findByIdIncludingDeleted(@Param("id") Long id);

    @Query("SELECT DISTINCT p.id FROM Team t JOIN t.players p WHERE t.tournament.id = :tournamentId")
    Set<Long> findPlayerIdsInTournament(@Param("tournamentId") Long tournamentId);

    @Query("SELECT p FROM Player p " +
            "LEFT JOIN FETCH p.account a " +
            "WHERE p.isDeleted = false")
    List<Player> findAllWithAccounts();

    @Query("SELECT DISTINCT p FROM Player p " +
            "LEFT JOIN FETCH p.playerRequests pr " +
            "LEFT JOIN FETCH pr.team t " +
            "LEFT JOIN FETCH pr.tournament tor " +
            "LEFT JOIN FETCH p.account a " +
            "WHERE p.isDeleted = false")
    List<Player> findAllWithRequestsAndAccounts();


    @Query("SELECT p FROM Player p WHERE p.account.username = :us AND p.isDeleted = false")
    Player findActiveByUsername(String us);
}
