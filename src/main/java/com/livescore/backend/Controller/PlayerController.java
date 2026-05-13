package com.livescore.backend.Controller;

import com.livescore.backend.DTO.PlayerDTO;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlayerController {
    @Autowired
    private PlayerService playerService;
    @PostMapping("/player")
    public ResponseEntity<?> createPlayer(@RequestBody PlayerDTO player) {
        return playerService.createPlayer(player);
    }
    @PutMapping("/player/{id}")
    public ResponseEntity<?> updatePlayer(@PathVariable Long id, @RequestBody PlayerDTO player) {
        return playerService.updatePlayer(id, player);
    }
    @DeleteMapping("/player/{id}")
    public ResponseEntity<?> deletePlayer(@PathVariable Long id) {
        return playerService.deletePlayer(id);
    }

    @PutMapping("/player/{id}/restore")
    public ResponseEntity<?> restorePlayer(@PathVariable Long id) {
        return playerService.restorePlayer(id);
    }
    @GetMapping("/player")
    public ResponseEntity<?> getAllPlayers() {
        return playerService.getAllPlayers();
    }
    @GetMapping("/player/{id}")
    public ResponseEntity<?> getPlayerById(@PathVariable Long id) {
        return playerService.getPlayerById(id);
    }

}
