package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Award;
import com.livescore.backend.Entity.FavouritePlayer;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.AwardInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.TournamentInterface;
import com.livescore.backend.Service.FavouritePlayerService;
import com.livescore.backend.Service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favourite-player")
@RequiredArgsConstructor
public class FavouritePlayerController {

    private final FavouritePlayerService fpService;
    private final MatchService matchService;
    private final TournamentInterface tournamentInterface;
    private final PlayerInterface playerInterface;
    private final AwardInterface awardInterface;

    /**
     * POST /api/favourite-player/vote
     * Body: { "matchId": 1, "accountId": 5, "playerId": 12 }
     */
    @PostMapping("/vote")
    public ResponseEntity<?> vote(@RequestBody Map<String, Object> body) {
        try {
            Long matchId   = body.get("matchId") != null ? ((Number) body.get("matchId")).longValue() : null;
            Long accountId = body.get("accountId") != null ? ((Number) body.get("accountId")).longValue() : null;
            Long playerId  = body.get("playerId") != null ? ((Number) body.get("playerId")).longValue() : null;
            String feedback = body.get("feedback") != null ? body.get("feedback").toString() : null;

            if (matchId == null || accountId == null || playerId == null) {
                return ResponseEntity.badRequest().body("matchId, accountId, playerId required");
            }

            // ✅ 4th argument pass karo
            String result = fpService.submitVote(matchId, accountId, playerId, feedback);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    /**
     * GET /api/favourite-player/results/{matchId}
     * Returns FavouritePlayer with playerVoteCounts map
     */
    @GetMapping("/results/{matchId}")
    public ResponseEntity<?> results(@PathVariable Long matchId) {
        try {
            FavouritePlayer fp = fpService.getResults(matchId);
            return ResponseEntity.ok(fp);
        } catch (RuntimeException e) {
            // No votes yet — return empty
            return ResponseEntity.ok(Map.of(
                    "playerVoteCounts", Map.of(),
                    "votedAccountIds",  new java.util.HashSet<>()
            ));
        }
    }

    /**
     * GET /api/favourite-player/check?matchId=1&accountId=5
     * Returns true/false whether this account already voted
     */
    @GetMapping("/check")
    public ResponseEntity<Boolean> check(
            @RequestParam Long matchId,
            @RequestParam Long accountId) {
        return ResponseEntity.ok(fpService.checkVote(matchId, accountId));
    }



    @GetMapping("/top-voted/{tournamentId}")
    public ResponseEntity<?> topVoted(@PathVariable Long tournamentId) {
        return ResponseEntity.ok(fpService.getTopVotedPlayersForTournament(tournamentId));
    }

    /**
     * POST /api/favourite-player/set-mot/{tournamentId}/{playerId}
     * Set Man of Tournament
     */
    // UNCOMMENT and change awardType to FAVOURITE_PLAYER:
    @PostMapping("/set-mot/{tournamentId}/{playerId}")
    public ResponseEntity<?> setFavouritePlayer(
            @PathVariable Long tournamentId,
            @PathVariable Long playerId) {
        try {
            var tournament = tournamentInterface.findById(tournamentId)
                    .orElseThrow(() -> new RuntimeException("Tournament not found"));
            var player = playerInterface.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Player not found"));

            // Find existing FAVOURITE_PLAYER award, or create new
            Award award = awardInterface.findByTournamentId(tournamentId).stream()
                    .filter(a -> "FAVOURITE_PLAYER".equals(a.getAwardType()))
                    .findFirst()
                    .orElseGet(() -> {
                        Award a = new Award();
                        a.setTournament(tournament);
                        a.setAwardType("FAVOURITE_PLAYER");
                        return a;
                    });

            award.setPlayer(player);
            award.setReason("Fan favourite - admin confirmed");
            awardInterface.save(award);

            return ResponseEntity.ok("Favourite Player set: " + player.getName());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/tournament/{tournamentId}/man-of-tournament")
    public ResponseEntity<?> setManOfTournament(
            @PathVariable Long tournamentId,
            @RequestParam Long playerId,
            @RequestParam(defaultValue = "1") int rank) {

        List<Award> existing = awardInterface.findAllByTournamentIdAndAwardType(tournamentId, "MAN_OF_TOURNAMENT");

        // ── 1. Delete the slot at this rank ──────────────────────────
        if (rank - 1 < existing.size()) {
            awardInterface.delete(existing.get(rank - 1));
            existing.remove(rank - 1); // local list bhi update karo
        }

        // ── 2. Remove this player from ANY other rank (dedup) ────────
        existing.stream()
                .filter(a -> a.getPlayer().getId().equals(playerId))
                .forEach(awardInterface::delete);

        Tournament t = tournamentInterface.findById(tournamentId).orElseThrow();
        Player p = playerInterface.findById(playerId).orElseThrow();

        Award a = new Award();
        a.setTournament(t);
        a.setPlayer(p);
        a.setAwardType("MAN_OF_TOURNAMENT");
        a.setReason("Admin selected - Rank " + rank);
        awardInterface.save(a);

        return ResponseEntity.ok().build();
    }
    @GetMapping("/top-stats/{tournamentId}")
    public ResponseEntity<?> topStats(@PathVariable Long tournamentId) {
        // Return current MAN_OF_TOURNAMENT awards (already top 3 from stats)
        List<Award> motAwards = awardInterface
                .findAllByTournamentIdAndAwardType(tournamentId, "MAN_OF_TOURNAMENT");

        List<Map<String, Object>> result = motAwards.stream().map(a -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("playerId", a.getPlayer().getId());
            m.put("playerName", a.getPlayer().getName());
            m.put("votes", a.getPointsEarned()); // points as "score"
            return m;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(result);
    }
}