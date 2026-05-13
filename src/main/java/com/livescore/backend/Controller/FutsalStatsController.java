package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Futsal.FutsalMatchStats;
import com.livescore.backend.Entity.Stats;
import com.livescore.backend.Sport.Futsal.FutsalStatsService;
import com.livescore.backend.Interface.StatsInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/futsal/stats")
@RequiredArgsConstructor
public class FutsalStatsController {

    private final FutsalStatsService futsalStatsService;
    private final StatsInterface statsInterface;

    /**
     * GET /api/futsal/stats/tournament/{tournamentId}
     * All players' tournament stats
     */
    @GetMapping("/tournament/{tournamentId}")
    public ResponseEntity<?> getTournamentStats(@PathVariable Long tournamentId) {
        List<Stats> stats = statsInterface.findAllByTournamentId(tournamentId);
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/futsal/stats/player/{playerId}/tournament/{tournamentId}
     * Single player's tournament stats
     */
    @GetMapping("/player/{playerId}/tournament/{tournamentId}")
    public ResponseEntity<?> getPlayerTournamentStats(
            @PathVariable Long playerId,
            @PathVariable Long tournamentId) {
        return statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/futsal/stats/player/{playerId}/history?tournamentId=1
     * Match-by-match progress for a player
     * Returns list of FutsalMatchStats (one row per match)
     */
    @GetMapping("/player/{playerId}/history")
    public ResponseEntity<List<FutsalMatchStats>> getPlayerHistory(
            @PathVariable Long playerId,
            @RequestParam Long tournamentId) {
        List<FutsalMatchStats> history =
                futsalStatsService.getPlayerMatchHistory(playerId, tournamentId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/futsal/stats/match/{matchId}
     * All players' stats in a specific match
     */
    @GetMapping("/match/{matchId}")
    public ResponseEntity<?> getMatchStats(@PathVariable Long matchId) {
        // FutsalMatchStatsInterface is in FutsalStatsService — expose via service
        // Or add FutsalMatchStatsInterface directly here if preferred
        return ResponseEntity.ok("Use /player/{id}/history for per-match data");
    }
}