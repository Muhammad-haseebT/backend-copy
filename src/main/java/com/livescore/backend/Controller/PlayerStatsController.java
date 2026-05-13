package com.livescore.backend.Controller;

import com.livescore.backend.DTO.PlayerFullStatsDTO;
import com.livescore.backend.Service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/player")
public class PlayerStatsController {
    @Autowired
    private StatsService statsService;

    @GetMapping("/{playerId}/stats")
    public ResponseEntity<PlayerFullStatsDTO> getPlayerStats(
            @PathVariable Long playerId,
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) String sport  // ✅ new param
    ) {
        PlayerFullStatsDTO dto = statsService.getPlayerFullStats(playerId, tournamentId, sport);
        return ResponseEntity.ok(dto);
    }

}
