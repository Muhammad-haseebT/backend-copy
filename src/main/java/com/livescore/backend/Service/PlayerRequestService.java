package com.livescore.backend.Service;


import com.livescore.backend.DTO.PlayerRequestDTO;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.PlayerRequest;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.PlayerRequestInterface;
import com.livescore.backend.Interface.TeamInterface;
import com.livescore.backend.Interface.TournamentInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PlayerRequestService {
    @Autowired
    PlayerRequestInterface playerRequestInterface;
    @Autowired
    PlayerInterface playerInterface;
    @Autowired
    TeamInterface teamInterface;
    @Autowired
    TournamentInterface tournamentInterface;
    @Autowired
    StatsService statsService;

    private String normalizeStatus(String status) {
        if (status == null) return null;
        String t = status.trim();
        if (t.isEmpty()) return null;
        return t.toUpperCase();
    }
    @CacheEvict(value = {"teamByTournamentId","teams","teamById","teamByTournamentIdAndAccountId","teamByPlayers"},allEntries = true)
    public ResponseEntity<?> createPlayerRequest(PlayerRequestDTO playerRequest) {
        if (playerRequest == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player request details are required")
            );
        }
        if(playerRequest.getPlayerId()==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player id is required")
            );
        }
        if(playerRequest.getTeamId()==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Team id is required")
            );
        }
        if(playerRequest.getTournamentId()==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Tournament id is required")
            );
        }
        Player player;

        if(playerRequest.getUs().isEmpty()){
            player=playerInterface.findActiveById(playerRequest.getPlayerId()).orElse(null);

        }
        else{
            player=playerInterface.findActiveByUsername(playerRequest.getUs());
        }
        Team team=teamInterface.findById(playerRequest.getTeamId()).orElse(null);
        Tournament tournament=tournamentInterface.findById(playerRequest.getTournamentId()).orElse(null);
        if(player==null||team==null||tournament==null){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player, Team or Tournament not found")
            );
        }

        PlayerRequest existing = playerRequestInterface.findExistingRequest(playerRequest.getPlayerId(), playerRequest.getTournamentId());

        if (existing != null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player already belongs to a team in this tournament")
            );
        }
        if(playerRequestInterface.existsByPlayerAndTournament(player, tournament,team)){
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player request already exists for this player and tournament")
            );
        }

        PlayerRequest playerRequest1=new PlayerRequest();
        playerRequest1.setPlayer(player);
        playerRequest1.setTeam(team);
        playerRequest1.setTournament(tournament);
        playerRequest1.setStatus("PENDING");
        playerRequestInterface.save(playerRequest1);
        return ResponseEntity.ok().build();



    }
    @CacheEvict(value = {"teamByTournamentId","teams","teamById","teamByTournamentIdAndAccountId","teamByPlayers"},allEntries = true)

    public ResponseEntity<?> updatePlayerRequest(Long id, PlayerRequestDTO playerRequest) {
        if (id == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "id is required")
            );
        }
        if (playerRequest == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player request details are required")
            );
        }
        PlayerRequest playerRequest1=playerRequestInterface.findById(id).orElse(null);
        if(playerRequest1==null){
            return ResponseEntity.notFound().build();
        }
        if (playerRequest.getPlayerId() != null) {
            playerRequest1.setPlayer(playerInterface.findActiveById(playerRequest.getPlayerId()).orElse(null));
        }
        if (playerRequest.getTeamId() != null) {
            playerRequest1.setTeam(teamInterface.findById(playerRequest.getTeamId()).orElse(null));
        }
        if (playerRequest.getTournamentId() != null) {
            playerRequest1.setTournament(tournamentInterface.findById(playerRequest.getTournamentId()).orElse(null));
        }
        String status = normalizeStatus(playerRequest.getStatus());
        if (status != null) {
            playerRequest1.setStatus(status);
        }
        playerRequestInterface.save(playerRequest1);
        return ResponseEntity.ok().build();
    }
    @CacheEvict(value = {"teamByTournamentId","teams","teamById","teamByTournamentIdAndAccountId","teamByPlayers"},allEntries = true)

    public ResponseEntity<?> deletePlayerRequest(Long id) {
        if(playerRequestInterface.existsById(id)){
            playerRequestInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    public ResponseEntity<?> getAllPlayerRequests() {
        return ResponseEntity.ok(playerRequestInterface.findAll());
    }
    public ResponseEntity<?> getPlayerRequestById(Long id) {
        return playerRequestInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @CacheEvict(value = {"teamByTournamentId","teams","teamById","teamByTournamentIdAndAccountId","teamByPlayers"},allEntries = true)

    public ResponseEntity<?> approvePlayerRequest(Long id) {
        PlayerRequest playerRequest=playerRequestInterface.findById(id).orElse(null);
        if(playerRequest==null){
            return ResponseEntity.notFound().build();
        }
        if (playerRequest.getTournament() == null || playerRequest.getTournament().getId() == null) {
            return ResponseEntity.badRequest().body("Tournament not found");
        }
        if (playerRequest.getTeam() == null || playerRequest.getTeam().getId() == null) {
            return ResponseEntity.badRequest().body("Team not found");
        }
        if (playerRequest.getPlayer() == null || Boolean.TRUE.equals(playerRequest.getPlayer().getIsDeleted())) {
            return ResponseEntity.badRequest().body("Player not found");
        }
        String status = normalizeStatus(playerRequest.getStatus());
        if("APPROVED".equals(status)){
            return ResponseEntity.badRequest().body("Player already approved");
        }
        PlayerRequest existing = playerRequestInterface.findExistingRequest(playerRequest.getPlayer().getId(), playerRequest.getTournament().getId());

        if (existing != null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Player already belongs to a team in this tournament")
            );
        }

        playerRequest.setStatus("APPROVED");
        playerRequestInterface.save(playerRequest);
        Player player=playerRequest.getPlayer();
        player.setTeam(playerRequest.getTeam());
        playerInterface.save(player);

        statsService.createStats(playerRequest.getPlayer().getId(),playerRequest.getTournament().getId());
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> getPlayerRequestsByPlayerId(Long playerId) {

        if (playerId == null) {
            return ResponseEntity.badRequest().body("playerId is required");
        }

        List<PlayerRequest> requests = playerRequestInterface.findbyPlayer_Id(playerId);

        List<Map<String, Object>> response = requests.stream().map(r -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("requestId", r.getId());
            map.put("teamName", r.getTeam() != null ? r.getTeam().getName() : null);

            // Team creator name (assumption: Team entity me creator/player/user ka relation maujood hai)
            // Example: team.getCreatedBy().getName()
            map.put("teamCreatorName",
                    (r.getTeam() != null && r.getTeam().getCreator() != null)
                            ? r.getTeam().getCreator().getName()
                            : null
            );
            map.put("status", r.getStatus());

            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @CacheEvict(value = {"teamByTournamentId","teams","teamById","teamByTournamentIdAndAccountId","teamByPlayers"},allEntries = true)

    public ResponseEntity<?> rejectPlayerRequest(Long id) {
        PlayerRequest playerRequest=playerRequestInterface.findById(id).orElse(null);
        if(playerRequest==null){
            return ResponseEntity.notFound().build();
        }
        String status = normalizeStatus(playerRequest.getStatus());
        if("REJECTED".equals(status)){
            return ResponseEntity.badRequest().body("Player already rejected");
        }
        playerRequest.setStatus("REJECTED");
        playerRequestInterface.save(playerRequest);
        return ResponseEntity.ok().build();
    }
}

