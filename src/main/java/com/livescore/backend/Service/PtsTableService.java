package com.livescore.backend.Service;

import com.livescore.backend.DTO.PtsTableDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PtsTableService {

    private final PtsTableInterface ptsTableInterface;
    private final MatchInterface matchInterface;
    private final CricketInningsInterface inningsInterface;

    // ==================== CREATE ====================

    public ResponseEntity<?> createPtsTable(PtsTable ptsTable) {

        if (ptsTable.getId() != null && ptsTableInterface.existsById(ptsTable.getId())) {
            return ResponseEntity.badRequest()
                    .body("PtsTable with id " + ptsTable.getId() + " already exists");
        }

        if (ptsTableInterface.existsByTeamIdAndTournamentId(
                ptsTable.getTeam().getId(),
                ptsTable.getTournament().getId())) {
            return ResponseEntity.badRequest().body(
                    "PtsTable with team id " + ptsTable.getTeam().getId() +
                            " and tournament id " + ptsTable.getTournament().getId() +
                            " already exists");
        }

        ptsTableInterface.save(ptsTable);
        return ResponseEntity.ok("PtsTable created successfully");
    }

    // ==================== UPDATE AFTER MATCH ====================

    @Transactional
    public ResponseEntity<?> updatePointsTableAfterMatch(Long matchId) {

        if (matchId == null || matchId <= 0) {
            return ResponseEntity.badRequest().body("Invalid matchId: must be positive");
        }

        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Match not found with id: " + matchId);
        }

        if (match.getWinnerTeam() == null) {
            return ResponseEntity.badRequest()
                    .body("Cannot update points: winner not set for matchId " + matchId);
        }

        if (match.getTeam1() == null || match.getTeam2() == null) {
            return ResponseEntity.badRequest().body("Match is missing Team1 or Team2 data");
        }

        if (match.getTournament() == null) {
            return ResponseEntity.badRequest().body("Tournament not found for this match");
        }

        Team winner = match.getWinnerTeam();
        Team loser  = match.getTeam1().getId().equals(winner.getId())
                ? match.getTeam2()
                : match.getTeam1();

        Long tournamentId = match.getTournament().getId();

        PtsTable ptsWinner = ptsTableInterface.findByTournamentIdAndTeamId(tournamentId, winner.getId());
        PtsTable ptsLoser  = ptsTableInterface.findByTournamentIdAndTeamId(tournamentId, loser.getId());

        if (ptsWinner == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("PtsTable not found for Winner Team ID: " + winner.getId());
        }
        if (ptsLoser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("PtsTable not found for Loser Team ID: " + loser.getId());
        }

        // --- Matches Played, Wins, Losses ---
        ptsWinner.setPlayed(safeInc(ptsWinner.getPlayed()));
        ptsLoser.setPlayed(safeInc(ptsLoser.getPlayed()));

        ptsWinner.setWins(safeInc(ptsWinner.getWins()));
        ptsLoser.setLosses(safeInc(ptsLoser.getLosses()));

        // --- Points (win = 2, loss = 0) ---
        ptsWinner.setPoints(safeAdd(ptsWinner.getPoints(), 2));
        // loser ke points mein kuch nahi jodna

        // --- NRR (har match ke baad full recalculate) ---
        // NOTE: NRR sirf tournament ke completed matches pe based hoti hai
        //       isliye har baar fresh calculate karo, cumulative mat karo
        double winnerNrr = calculateTeamNrr(winner.getId(), tournamentId);
        double loserNrr  = calculateTeamNrr(loser.getId(),  tournamentId);

        ptsWinner.setNrr(winnerNrr);
        ptsLoser.setNrr(loserNrr);

        ptsTableInterface.save(ptsWinner);
        ptsTableInterface.save(ptsLoser);

        // --- Response ---
        Map<String, Object> response = new HashMap<>();
        response.put("message",       "Points table updated successfully");
        response.put("winnerTeamId",  winner.getId());
        response.put("loserTeamId",   loser.getId());
        response.put("winnerPoints",  ptsWinner.getPoints());
        response.put("loserPoints",   ptsLoser.getPoints());
        response.put("winnerNRR",     winnerNrr);
        response.put("loserNRR",      loserNrr);

        return ResponseEntity.ok(response);
    }

    // ==================== NRR CALCULATION (FIXED) ====================

    /**
     * NRR = (Total Runs Scored / Total Overs Faced)
     *       - (Total Runs Conceded / Total Overs Bowled)
     *
     * All-Out Rule (ICC standard):
     *   - Agar batting team all-out hui (10 wickets) →
     *     overs = match ke allotted overs (e.g. 20.0 for T20)
     *   - Agar batting team ne target chase kiya (jeet gayi) →
     *     overs = actual overs used
     *   - Normal (overs complete, no wicket out) →
     *     overs = actual overs played
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public double calculateTeamNrr(Long teamId, Long tournamentId) {

        if (teamId == null || tournamentId == null) return 0.0;

        List<Match> matches = matchInterface.findCompletedMatchesByTeam(teamId, tournamentId);
        if (matches == null || matches.isEmpty()) return 0.0;

        double runsScored   = 0.0;
        double oversFaced   = 0.0;
        double runsConceded = 0.0;
        double oversBowled  = 0.0;

        for (Match match : matches) {
            if (match == null) continue;

            int maxOvers = match.getOvers(); // T20 = 20, ODI = 50

            List<CricketInnings> myList  = inningsInterface.findByMatchAndTeamList(match.getId(), teamId);
            List<CricketInnings> oppList = inningsInterface.findOpponentInningsList(match.getId(), teamId);

            if (myList.isEmpty() || oppList.isEmpty()) continue;

            CricketInnings myInnings  = myList.get(0);
            CricketInnings oppInnings = oppList.get(0);




            if (myInnings == null || oppInnings == null) continue;



            InningsData self = calculateInningsStats(myInnings, maxOvers);
            InningsData opp  = calculateInningsStats(oppInnings, maxOvers);

            // Overs faced by MY team (batting)
            // All-out → full allotted overs
            // Chased successfully / normal → actual overs
            double myOvers  = self.allOut ? maxOvers : self.overs;

            // Overs bowled by MY team (opponent batting)
            double oppOvers = opp.allOut ? maxOvers : opp.overs;

            if (myOvers <= 0 || oppOvers <= 0) continue;

            runsScored   += self.runs;
            oversFaced   += myOvers;
            runsConceded += opp.runs;
            oversBowled  += oppOvers;
        }

        if (oversFaced <= 0.0 || oversBowled <= 0.0) return 0.0;

        double nrr = (runsScored / oversFaced) - (runsConceded / oversBowled);
        return roundToThreeDecimals(nrr);
    }

    // ==================== INNINGS STATS HELPER ====================

    /**
     * Ek innings se runs, overs, aur allOut status nikalta hai.
     *
     * @param innings   CricketInnings entity
     * @param maxOvers  Match ke allotted overs (needed for allOut check)
     */
    private InningsData calculateInningsStats(CricketInnings innings, int maxOvers) {
        if (innings == null) return new InningsData();

        List<CricketBall> balls = innings.getBalls();
        if (balls == null || balls.isEmpty()) return new InningsData();

        int totalRuns  = 0;
        int legalBalls = 0;
        int wickets    = 0;

        for (CricketBall ball : balls) {
            if (ball == null) continue;

            // Runs: bat runs + extras dono count hote hain NRR mein
            totalRuns += safeInt(ball.getRuns()) + safeInt(ball.getExtra());

            // Legal delivery: wide aur no-ball count nahi hoti overs mein
            if (isLegalDelivery(ball.getExtraType())) {
                legalBalls++;
            }

            // Wicket count for all-out detection
            if (ball.getDismissalType() != null && !ball.getDismissalType().isBlank()) {
                wickets++;
            }
        }

        // Decimal overs: 19 balls = 3 overs 1 ball = 3.1
        // (NOT 19/6 = 3.166... — cricket notation hai)
        int    completedOvers   = legalBalls / 6;
        int    remainingBalls   = legalBalls % 6;
        double overs = completedOvers + (remainingBalls / 10.0); // e.g. 3.1, 18.4, 20.0

        // All out = 10 wickets gire
        boolean allOut = (wickets >= 10);

        return new InningsData(totalRuns, overs, allOut);
    }

    // ==================== INNINGS DATA (Inner Class) ====================

    private static class InningsData {
        int     runs   = 0;
        double  overs  = 0.0;
        boolean allOut = false;

        public InningsData() {}

        public InningsData(int runs, double overs, boolean allOut) {
            this.runs   = runs;
            this.overs  = overs;
            this.allOut = allOut;
        }
    }

    // ==================== READ METHODS ====================

    public ResponseEntity<?> deletePtsTable(Long id) {
        if (ptsTableInterface.existsById(id)) {
            ptsTableInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    public ResponseEntity<?> getAllPtsTables() {
        return ResponseEntity.ok(ptsTableInterface.findAll());
    }

    public ResponseEntity<?> getPtsTableById(Long id) {
        return ptsTableInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> getPointsTableByTournamentIdAndTeamId(Long tournamentId, Long teamId) {
        return ResponseEntity.ok(
                ptsTableInterface.findByTournamentIdAndTeamId(tournamentId, teamId));
    }

    /**
     * Tournament ki points table return karta hai —
     * sorted by: Points (desc) → NRR (desc)
     */
    public ResponseEntity<?> getPtsTablesByTournament(Long tournamentId) {
        List<PtsTable> tables = ptsTableInterface.findByTournamentId(tournamentId);

        List<PtsTableDTO> dtoList = tables.stream()
                .map(pt -> {
                    PtsTableDTO dto = new PtsTableDTO();
                    dto.setId(pt.getId());
                    dto.setTeamId(pt.getTeam().getId());
                    dto.setTeamName(pt.getTeam().getName());
                    dto.setTournamentId(pt.getTournament().getId());
                    dto.setPlayed(pt.getPlayed());
                    dto.setWins(pt.getWins());
                    dto.setLosses(pt.getLosses());
                    dto.setPoints(pt.getPoints());
                    dto.setNrr(pt.getNrr());
                    dto.setSport("cricket");
                    return dto;
                })
                // Points zyada → pehle; tie mein NRR zyada → pehle
                .sorted(Comparator
                        .comparingInt(PtsTableDTO::getPoints).reversed()
                        .thenComparingDouble(PtsTableDTO::getNrr).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    // ==================== PRIVATE HELPERS ====================

    private boolean isLegalDelivery(String extraType) {
        if (extraType == null) return true;
        String t = extraType.trim().toUpperCase();
        // Wide aur No-Ball legal delivery nahi hoti
        return !(t.equals("WIDE") || t.equals("NO_BALL") || t.equals("NOBALL"));
    }

    private double roundToThreeDecimals(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) return 0.0;
        return BigDecimal.valueOf(val)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private int safeInc(Integer x) {
        return (x == null) ? 1 : x + 1;
    }

    private int safeAdd(Integer x, int add) {
        return (x == null) ? add : x + add;
    }

    private int safeInt(Integer x) {
        return x == null ? 0 : x;
    }
}