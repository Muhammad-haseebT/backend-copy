package com.livescore.backend.Controller;

import com.livescore.backend.DTO.PlayerRequestDTO;
import com.livescore.backend.Entity.PlayerRequest;
import com.livescore.backend.Service.PlayerRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlayerRequestController {
    @Autowired
    private PlayerRequestService playerRequestService;
    @PostMapping("/playerRequest")
    public ResponseEntity<?> createPlayerRequest(@RequestBody PlayerRequestDTO playerRequest) {
        return playerRequestService.createPlayerRequest(playerRequest);
    }
    @PutMapping("/playerRequest/{id}")
    public ResponseEntity<?> updatePlayerRequest(@PathVariable Long id, @RequestBody PlayerRequestDTO playerRequest) {
        return playerRequestService.updatePlayerRequest(id, playerRequest);
    }
    @DeleteMapping("/playerRequest/{id}")
    public ResponseEntity<?> deletePlayerRequest(@PathVariable Long id) {
        return playerRequestService.deletePlayerRequest(id);
    }
    @GetMapping("/playerRequest")
    public ResponseEntity<?> getAllPlayerRequests() {
        return playerRequestService.getAllPlayerRequests();
    }
    @GetMapping("/playerRequest/{id}")
    public ResponseEntity<?> getPlayerRequestById(@PathVariable Long id) {
        return playerRequestService.getPlayerRequestById(id);
    }
    //approve request
    @PutMapping("/playerRequest/approve/{id}")
    public ResponseEntity<?> approvePlayerRequest(@PathVariable Long id) {
        return playerRequestService.approvePlayerRequest(id);
    }
    //reject request
    @PutMapping("/playerRequest/reject/{id}")
    public ResponseEntity<?> rejectPlayerRequest(@PathVariable Long id) {
        return playerRequestService.rejectPlayerRequest(id);
    }

    @GetMapping("/playerRequest/player/{playerId}")
    public ResponseEntity<?> getPlayerRequestsByPlayerId(@PathVariable Long playerId) {
        return playerRequestService.getPlayerRequestsByPlayerId(playerId);
    }
}
