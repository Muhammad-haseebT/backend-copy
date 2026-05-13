package com.livescore.backend.Controller;

import com.livescore.backend.DTO.TournamentAwardsDTO;
import com.livescore.backend.Service.AwardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tournament")
public class TournamentStatsController {

    @Autowired
    private AwardService awardService;

    /**
     * Get tournament stats + awards (view anytime)
     */
    @GetMapping("/{tournamentId}/stats")
    public ResponseEntity<?> getTournamentStats(@PathVariable Long tournamentId) {
        TournamentAwardsDTO dto = awardService.getTournamentStats(tournamentId);
        if (dto == null) {
            return ResponseEntity.badRequest().body("Tournament not found");
        }
        return ResponseEntity.ok(dto);
    }
    @PostMapping("/{id}/recalculate")
    public ResponseEntity<?> recalculate(@PathVariable Long id) {
        return ResponseEntity.ok(awardService.recalculateAndGetStats(id));
    }
    /**
     * End tournament — calculate all awards and return results
     * Call this once when tournament is over
//     */
//    @PostMapping("/{tournamentId}/end")
//    public ResponseEntity<?> endTournament(@PathVariable Long tournamentId) {
//        TournamentAwardsDTO dto = awardService.endTournamentAndGenerateAwards(tournamentId);
//        if (dto == null) {
//            return ResponseEntity.badRequest().body("Tournament not found");
//        }
//        return ResponseEntity.ok(dto);
//    }
}