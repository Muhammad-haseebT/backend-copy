package com.livescore.backend.Service;

import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.PlayerRequest;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.*;
import com.livescore.backend.Util.Constants;
import com.livescore.backend.Util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TeamService {
    @Autowired
    private TeamInterface teamInterface;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlayerInterface playerInterface;
    @Autowired
    private PlayerRequestInterface pri;

    @Autowired
    private TournamentInterface tournamentInterface;
    @Autowired
    private PlayerRequestInterface playerRequestInterface;


    @CacheEvict(value = {"teamByTournamentId", "teams", "teamById", "teamByTournamentIdAndAccountId", "teamByPlayers"}, allEntries = true)
    public ResponseEntity<?> createTeam(Team team, Long tournamentId, Long playerId) {
        // Validate input
        ResponseEntity<?> validation = ValidationUtils.validateNotNull(team, "Team details");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequiredId(tournamentId, "Tournament id");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequiredId(playerId, "Player id");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequired(team.getName(), "Team name");
        if (validation != null) return validation;
        Optional<Tournament> tournamentOpt = tournamentInterface.findById(tournamentId);
        if (tournamentOpt.isEmpty()) {
            return ValidationUtils.badRequest("Tournament not found with ID: " + tournamentId);
        }
        Optional<Player> playerOpt = playerInterface.findActiveById(playerId);
        if (playerOpt.isEmpty()) {
            return ValidationUtils.badRequest("Player not found with ID: " + playerId);
        }

        if(playerRequestInterface.findExistingRequest(playerId,tournamentId)!=null){
            return ValidationUtils.badRequest("Player already in an other team");
        }
        Player p1 = playerOpt.get();
        team.setTournament(tournamentOpt.get());
        team.setCreator(p1);
        List<Player> players = teamInterface.findPlayersByteamId(team.getId());
        players.add(p1);
        team.setPlayers(players);
        Team savedTeam = teamInterface.save(team);
        p1.setPlayerRole(Constants.ROLE_CAPTAIN);
        playerInterface.save(p1);


        PlayerRequest playerRequest = new PlayerRequest();
        playerRequest.setPlayer(playerOpt.get());
        playerRequest.setTeam(savedTeam);
        playerRequest.setTournament(tournamentOpt.get());
        playerRequest.setStatus(Constants.STATUS_APPROVED);
        pri.save(playerRequest);

        return ResponseEntity.ok(Map.of(
                "message", "Team created successfully",
                "teamId", savedTeam.getId(),
                "name", savedTeam.getName(),
                "tournamentId", savedTeam.getTournament().getId()
        ));


    }

    @CacheEvict(value = {"teamByTournamentId", "teams", "teamById", "teamByTournamentIdAndAccountId", "teamByPlayers"}, allEntries = true)
    public ResponseEntity<?> updateTeam(Long id, Team team) {
        ResponseEntity<?> validation = ValidationUtils.validateNotNull(team, "Team details");
        if (validation != null) return validation;
        return teamInterface.findById(id).map(teamEntity -> {
            if (team.getName() != null && !team.getName().isBlank()) {
                teamEntity.setName(team.getName());
            }
            if (team.getStatus() != null && !team.getStatus().isBlank()) {
                teamEntity.setStatus(team.getStatus());
            }
            return ResponseEntity.ok(teamInterface.save(teamEntity));
        }).orElse(ResponseEntity.notFound().build());
    }

    @CacheEvict(value = {"teamByTournamentId", "teams", "teamById", "teamByTournamentIdAndAccountId", "teamByPlayers"}, allEntries = true)
    public ResponseEntity<?> deleteTeam(Long id) {
        if (teamInterface.existsById(id)) {
            teamInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Cacheable(value = "teams", key = "#tid")
    public ResponseEntity<?> getAllTeams() {
        return ResponseEntity.ok(teamInterface.findAll());
    }

    @Cacheable(value = "teamById", key = "#tid")

    public ResponseEntity<?> getTeamById(Long id) {
        return teamInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @Cacheable(value = "teamByTournamentId", key = "#tid")
    public ResponseEntity<?> getTeamByTournamentId(Long tid) {
        List<Map<String, Object>> response = new ArrayList<>();
        ResponseEntity<?> validation = ValidationUtils.validateRequiredId(tid, "Tournament id");
        if (validation != null) return validation;
        List<Team> teams = teamInterface.findByTournamentId(tid);
        if (teams == null || teams.isEmpty()) {
            return ResponseEntity.ok(response);
        }

        for (Team team : teams) {
            if (team == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", team.getId());
            m.put("name", team.getName());
            m.put("status", team.getStatus());
            response.add(m);
        }

        return ResponseEntity.ok(response);

    }

    @Cacheable(value = "teamByTournamentIdAndAccountId", key = "T(java.util.Objects).hash(#tid,#aid)")

    public ResponseEntity<?> getTeamByTournamentIdAndAccountId(Long tid, Long aid) {
        // Validate input
        if (tid == null || aid == null) {
            return ValidationUtils.badRequest("Tournament id and account id are required");
        }

        // Get player ID
        Long creatorPlayerId = playerInterface.findByAccount_Id(aid)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(Player::getId)
                .orElse(null);

        if (creatorPlayerId == null) {
            return ResponseEntity.notFound().build();
        }

        // Get team
        Optional<Team> teamOpt = teamInterface.findByTournamentIdAndPlayerId(tid, creatorPlayerId);
        if (teamOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Team team = teamOpt.get();

        // OPTIMIZED: Single query with JOIN FETCH
        List<PlayerRequest> players = pri
                .findByTeamIdWithPlayer(team.getId());

        List<Map<String, Object>> playersLite = players.stream()
                .filter(p -> p != null && p.getPlayer() != null)
                .filter(p -> !Boolean.TRUE.equals(p.getPlayer().getIsDeleted()))
                .map(p -> {
                    Map<String, Object> playerMap = new HashMap<>();
                    playerMap.put("id", p.getId());
                    playerMap.put("name", p.getPlayer().getName());
                    playerMap.put("status", p.getStatus());
                    return playerMap;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("teamName", team.getName());
        response.put("teamId", team.getId());
        response.put("teamStatus", team.getStatus());
        response.put("players", playersLite);

        return ResponseEntity.ok(response);
    }

    @Cacheable(value = "teamByPlayers", key = "#teamId")

    public ResponseEntity<?> findPlayersByTeam(Long teamId) {
        // Team exist karta hai?
        Team team = teamInterface.findById(teamId).orElse(null);
        if (team == null) {
            return ResponseEntity.notFound().build();
        }

        // player_request se APPROVED players fetch karo
        List<Player> players = playerRequestInterface
                .findApprovedPlayersByTeamId(teamId);

        return ResponseEntity.ok(players);
    }
}
