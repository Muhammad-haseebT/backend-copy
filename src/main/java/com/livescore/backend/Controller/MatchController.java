package com.livescore.backend.Controller;
import com.livescore.backend.DTO.MatchDTO;
import com.livescore.backend.Entity.FavouritePlayer;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Service.FavouritePlayerService;
import com.livescore.backend.Service.MatchService;
import com.livescore.backend.Service.PlayerInningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MatchController {
    @Autowired
    private MatchService matchService;
    @Autowired
    private MatchInterface matchInterface;
    @Autowired
    private PlayerInningsService playerInningsService;
    @Autowired
    private final FavouritePlayerService favouritePlayerService;

    @PostMapping("/match")
    public ResponseEntity<?> createMatch(@RequestBody MatchDTO matchDTO) {
        return matchService.createMatch(matchDTO);
    }

    @PutMapping("/match/{id}")
    public ResponseEntity<?> updateMatch(@PathVariable Long id, @RequestBody MatchDTO matchDTO) {
        return matchService.updateMatch(id, matchDTO);
    }

    @DeleteMapping("/match/{id}")
    public ResponseEntity<?> deleteMatch(@PathVariable Long id) {
        return matchService.deleteMatch(id);
    }

    @GetMapping("/match/{id}")
    public ResponseEntity<?> getMatch(@PathVariable Long id) {
        return matchService.getMatch(id);
    }

    @GetMapping("/match")
    public ResponseEntity<?> getAllMatches() {
        return matchService.getAllMatches();
    }

    @GetMapping("/match/tournament/{tournamentId}")
    public ResponseEntity<?> getMatchesByTournament(@PathVariable Long tournamentId) {
        return matchService.getMatchesByTournament(tournamentId);
    }

    @GetMapping("/match/team/{teamId}")
    public ResponseEntity<?> getMatchesByTeam(@PathVariable Long teamId) {
        return matchService.getMatchesByTeam(teamId);
    }

    @GetMapping("/match/status/{status}")
    public ResponseEntity<?> getMatchesByStatus(@PathVariable String status) {
        return matchService.getMatchesByStatus(status);
    }




    @GetMapping("/match/date/{date}")
    public ResponseEntity<?> getMatchesByDate(@PathVariable LocalDate date) {
        return matchService.getMatchesByDate(date);
    }

    @GetMapping("/match/time/{time}")
    public ResponseEntity<?> getMatchesByTime(@PathVariable LocalTime time) {
        return matchService.getMatchesByTime(time);
    }
    //start match (status changed to live ,scorerID assign ho ,tosswinner id,decision assign ho)
    @PutMapping("/match/start/{id}")
    public ResponseEntity<?> startMatch(@PathVariable Long id,@RequestBody MatchDTO match) {
        return matchService.startMatch(id,match);
    }

    @PutMapping("/match/end/{id}")
    public ResponseEntity<?> endMatch(@PathVariable Long id) {
        return matchService.endMatch(id);
    }

    @PutMapping("/match/abandon/{id}")
    public ResponseEntity<?> abandonMatch(@PathVariable Long id) {
        return matchService.abandonMatch(id);
    }

    @GetMapping("/match/sport")
    public ResponseEntity<?> getMatchesBySport(
            @RequestParam(required = false) String name,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(matchService.getmatchbystatusandSport(name, status));
    }

    @GetMapping("/match/scorer/{id}")
    public ResponseEntity<?> getMatchesByScorer(@PathVariable Long id){
        return matchService.getMatchesByScorer(id);
    }



    @GetMapping("/match/scoreCard/{Mid}/{T1id}")
    public ResponseEntity<?> getScoreCard(@PathVariable Long Mid,@PathVariable Long T1id){
        return playerInningsService.getScorecard(Mid,T1id);
    }

    @GetMapping("/match/summary/{mid}")
    public ResponseEntity<?> getMatchSummary(@PathVariable Long mid){
        return  playerInningsService.getSummary(mid);
    }

    @GetMapping("/match/balls/{mid}/{tid}")
    public ResponseEntity<?> getMatchBalls(@PathVariable Long mid,@PathVariable Long tid){
        return  playerInningsService.getMatchBalls(mid,tid);
    }

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

            String result = favouritePlayerService.submitVote(matchId, accountId, playerId, feedback);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }


    @GetMapping("/results/{matchId}")
    public ResponseEntity<FavouritePlayer> results(@PathVariable Long matchId) {
        return ResponseEntity.ok(favouritePlayerService.getResults(matchId));
    }
    @GetMapping("/results/{matchId}/{aid}")
    public ResponseEntity<?> results(@PathVariable Long matchId,@PathVariable Long aid) {
        return ResponseEntity.ok(favouritePlayerService.checkVote(matchId, aid));
    }



}
