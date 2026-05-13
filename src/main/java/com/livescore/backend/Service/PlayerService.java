package com.livescore.backend.Service;

import com.livescore.backend.DTO.PlayerDTO;
import com.livescore.backend.DTO.ShowRequestDTO;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.PlayerRequest;
import com.livescore.backend.Interface.AccountInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.PlayerRequestInterface;
import com.livescore.backend.Util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PlayerService {
    @Autowired
    private PlayerInterface playerInterface;
    @Autowired
    private AccountInterface accountInterface;
    @Autowired
    private PlayerRequestInterface playerRequestInterface;
    public ResponseEntity<?> createPlayer(PlayerDTO player) {
        // Validate input
        ResponseEntity<?> validation = ValidationUtils.validateNotNull(player, "Player details");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequired(player.getName(), "Player name");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequired(player.getPlayerRole(), "Player role");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequired(player.getUsername(), "Username");
        if (validation != null) return validation;
        Player p1=new Player();
        p1.setName(player.getName());
        p1.setPlayerRole(player.getPlayerRole());

        // Check if player already exists
        if(playerInterface.existsByAccount_Username(player.getUsername())){
            return ValidationUtils.badRequest("Player with this username already exists");
        }

        // Verify account exists
        var account = accountInterface.findByUsername(player.getUsername());
        if (account == null) {
            return ValidationUtils.badRequest("Account not found");
        }
        p1.setAccount(account);

        Player savedPlayer = playerInterface.save(p1);
        return ResponseEntity.ok(Map.of(
                "message", "Player created successfully",
                "playerId", savedPlayer.getId(),
                "name", savedPlayer.getName()
        ));
    }
    public ResponseEntity<?> updatePlayer(Long id, PlayerDTO player) {
        // Validate input
        ResponseEntity<?> validation = ValidationUtils.validateRequiredId(id, "Player id");
        if (validation != null) return validation;

        validation = ValidationUtils.validateNotNull(player, "Player details");
        if (validation != null) return validation;
        return playerInterface.findActiveById(id).map(playerEntity -> {
            if (player.getName() != null && !player.getName().isBlank()) {
                playerEntity.setName(player.getName());
            }
            if (player.getPlayerRole() != null && !player.getPlayerRole().isBlank()) {
                playerEntity.setPlayerRole(player.getPlayerRole());
            }
            return ResponseEntity.ok(playerInterface.save(playerEntity));
        }).orElse(ResponseEntity.notFound().build());
    }
    public ResponseEntity<?> deletePlayer(Long id) {
        return playerInterface.findActiveById(id)
                .map(p -> {
                    p.softDelete();
                    playerInterface.save(p);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
    public ResponseEntity<?> getAllPlayers() {

        // Use optimized repository method to avoid N+1 queries
        List<Player> players = playerInterface.findAllWithRequestsAndAccounts();
        List<PlayerDTO> result = new ArrayList<>();

        for (Player i : players) {

            PlayerDTO p2 = new PlayerDTO();
            p2.setId(i.getId());
            p2.setName(i.getName());
            p2.setPlayerRole(i.getPlayerRole());

            p2.setPlayerRequests(new ArrayList<>());

            // playerRequests should already be fetched because of the JOIN FETCH in the query
            if (i.getPlayerRequests() != null) {
                for (PlayerRequest j : i.getPlayerRequests()) {

                    if (j == null) continue;

                    ShowRequestDTO pr1 = new ShowRequestDTO();
                    pr1.setRequestId(j.getId());
                    pr1.setStatus(j.getStatus());

                    if (j.getTeam() != null) {
                        pr1.setTeamId(j.getTeam().getId());
                        pr1.setTeamName(j.getTeam().getName());
                    }

                    if (j.getTournament() != null) {
                        pr1.setTournamentId(j.getTournament().getId());
                    }

                    p2.getPlayerRequests().add(pr1);
                }
            }

            result.add(p2);
        }

        return ResponseEntity.ok(result);
    }

    public ResponseEntity<?> getPlayerById(Long id) {
        return playerInterface.findActiveById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> restorePlayer(Long id) {
        ResponseEntity<?> validation = ValidationUtils.validateRequiredId(id, "Player id");
        if (validation != null) return validation;
        Player p = playerInterface.findByIdIncludingDeleted(id).orElse(null);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        p.restore();
        playerInterface.save(p);
        return ResponseEntity.ok().build();
    }




}
