package com.livescore.backend.Controller;

import com.livescore.backend.DTO.PlayerFullStatsDTO;
import com.livescore.backend.Service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/player")
@CrossOrigin
public class PlayerStatsController {
    @Autowired
    private StatsService statsService;

    @GetMapping("/{playerId}/stats")
    public ResponseEntity<PlayerFullStatsDTO> getPlayerStats(
            @PathVariable Long playerId,
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) String sport
    ) {
        PlayerFullStatsDTO dto = statsService.getPlayerFullStats(playerId, tournamentId, sport);
        return ResponseEntity.ok(dto);
    }

    /**
     * Force recalculation of ALL Stats rows from raw CricketBall data.
     * Call once after code changes to refresh stale stats.
     */
    @PostMapping("/stats/recalculate-all")
    public ResponseEntity<String> recalculateAllStats() {
        statsService.recalculateAllPlayerStats();
        return ResponseEntity.ok("All player stats recalculated successfully.");
    }
}
