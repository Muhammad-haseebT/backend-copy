package com.livescore.backend.Service;

import com.livescore.backend.DTO.PlayerFullStatsDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsInterface statsInterface;
    private final PlayerInterface playerInterface;
    private final CricketBallInterface cricketBallInterface;
    private final MatchInterface matchRepo;
    private final TournamentInterface tournamentInterface;
    private final AwardInterface awardInterface;
    private final CricketInningsInterface cricketInningsInterface; // Add this to your interfaces

    // ==================== STATS CREATION ====================

    private Stats createInitializedStats(Tournament tournament, Player player, Sports sport) {
        Stats s = new Stats();
        s.setTournament(tournament);
        s.setPlayer(player);
        s.setSportType(sport);
        s.setRuns(0);
        s.setWickets(0);
        s.setHighest(0);
        s.setNotOut(0);
        s.setStrikeRate(0);
        s.setBattingAverage(0.0);
        s.setBallsFaced(0);
        s.setBallsBowled(0);
        s.setRunsConceded(0);
        s.setFours(0);
        s.setSixes(0);
        s.setFifties(0);
        s.setHundreds(0);
        s.setInningsPlayed(0);
        s.setMaidens(0);
        s.setThreeWicketHauls(0);
        s.setFiveWicketHauls(0);
        s.setDotBalls(0);
        s.setCatches(0);
        s.setRunouts(0);
        s.setStumpings(0);
        s.setPlayerOfMatchCount(0);
        s.setPoints(0);
        s.setEconomy(0.0);
        s.setBowlingAverage(0.0);
        s.setBowlingStrikeRate(0.0);
        s.setGoals(0);
        s.setAssists(0);
        s.setFouls(0);
        s.setYellowCards(0);
        s.setRedCards(0);
        return s;
    }

    public void createStats(Long playerId, Long tournamentId) {
        if (playerId == null || tournamentId == null) return;
        if (statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).isPresent()) return;

        Player player = playerInterface.findActiveById(playerId).orElse(null);
        if (player == null) return;

        Tournament t = tournamentInterface.findById(tournamentId).orElse(null);
        if (t == null) return;

        Stats stats = createInitializedStats(t, player, t.getSport());
        statsInterface.save(stats);
    }

    // ==================== END OF MATCH HOOK ====================

    @Transactional
    public void onMatchEnd(Long matchId) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null) return;

        Long tournamentId = match.getTournament().getId();

        // 1. Gather all player IDs from this match
        Set<Long> playerIds = new HashSet<>();
        for (CricketInnings ci : match.getCricketInnings()) {
            List<CricketBall> balls = cricketBallInterface.findAllByInningsAndMatch(ci.getId(), matchId);
            for (CricketBall b : balls) {
                if (b.getBatsman() != null)   playerIds.add(b.getBatsman().getId());
                if (b.getBowler() != null)    playerIds.add(b.getBowler().getId());
                if (b.getFielder() != null)   playerIds.add(b.getFielder().getId());
            }
        }

        // 2. Ensure Stats rows exist, then recalculate
        for (Long pid : playerIds) {
            createStats(pid, tournamentId);
            recalculatePlayerStats(pid, tournamentId);
        }

        // 3. Player of the Match
        calculatePlayerOfMatch(matchId);

        // 4. Check if tournament is over — generate tournament awards
        checkAndHandleTournamentEnd(match.getTournament());
    }

    // ==================== FULL RECALCULATION ====================

    /**
     * Wipes & rewrites a player's tournament stats from scratch using
     * every CricketBall row tied to this tournament.
     *
     * Logic:
     *  - For each innings in the tournament, collect balls where player was batsman/bowler/fielder
     *  - Batting:  runs, ballsFaced, 4s, 6s, dismissal, highest, 50s, 100s, notOuts, SR, avg
     *  - Bowling:  wickets, runsConceded, ballsBowled, economy, avg, SR, maidens, dotBalls, 3W/5W hauls
     *  - Fielding: catches, stumpings, runouts
     */
    @Transactional
    public void recalculatePlayerStats(Long playerId, Long tournamentId) {
        Stats stats = statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).orElse(null);
        if (stats == null) return;

        // Fetch ALL balls for this player in this tournament at once
        List<CricketBall> allBalls = cricketBallInterface
                .findAllByPlayerAndTournament(playerId, tournamentId);
        // You need this query: SELECT b FROM CricketBall b WHERE b.innings.match.tournament.id = :tid
        //   AND (b.batsman.id = :pid OR b.bowler.id = :pid OR b.fielder.id = :pid)

        // -------- BATTING --------
        // Group balls by innings where this player was batting (as batsman)
        Map<Long, List<CricketBall>> battingByInnings = allBalls.stream()
                .filter(b -> b.getBatsman() != null && b.getBatsman().getId().equals(playerId))
                .collect(Collectors.groupingBy(b -> b.getInnings().getId()));

        int totalRuns = 0, totalBallsFaced = 0, totalFours = 0, totalSixes = 0;
        int highest = 0, fifties = 0, hundreds = 0, notOuts = 0, inningsPlayed = 0;

        for (Map.Entry<Long, List<CricketBall>> entry : battingByInnings.entrySet()) {
            List<CricketBall> inningsBalls = entry.getValue();

            int inningsRuns = 0, inningsBalls_ = 0, inningsFours = 0, inningsSixes = 0;
            boolean dismissed = false;

            for (CricketBall b : inningsBalls) {
                // Runs scored by bat (exclude byes/leg-byes which go off bat but are extras)
                if (!isByeOrLegBye(b)) {
                    inningsRuns += safeInt(b.getRuns());
                }

                // Ball faced: only legal deliveries (not wides)
                if (!isWide(b)) {
                    inningsBalls_++;
                }

                if (Boolean.TRUE.equals(b.getIsFour())) inningsFours++;
                if (Boolean.TRUE.equals(b.getIsSix()))  inningsSixes++;

                // Check dismissal — outBatsman lets us know WHO got out
                if (b.getDismissalType() != null && b.getOutPlayer() != null
                        && b.getOutPlayer().getId().equals(playerId)) {
                    dismissed = true;
                }
            }

            inningsPlayed++;
            totalRuns       += inningsRuns;
            totalBallsFaced += inningsBalls_;
            totalFours      += inningsFours;
            totalSixes      += inningsSixes;

            if (inningsRuns > highest)  highest = inningsRuns;
            if (inningsRuns >= 100)     hundreds++;
            else if (inningsRuns >= 50) fifties++;
            if (!dismissed)             notOuts++;
        }

        // Batting Strike Rate = (runs / balls faced) * 100
        double strikeRate = totalBallsFaced > 0
                ? roundTo2((double) totalRuns / totalBallsFaced * 100) : 0.0;

        // Batting Average = runs / (innings - notOuts)   [if all notOut → use innings]
        int dismissals = inningsPlayed - notOuts;
        double battingAvg = dismissals > 0
                ? roundTo2((double) totalRuns / dismissals) : (inningsPlayed > 0 ? totalRuns : 0);

        // -------- BOWLING --------
        // Group by innings where this player was bowler
        Map<Long, List<CricketBall>> bowlingByInnings = allBalls.stream()
                .filter(b -> b.getBowler() != null && b.getBowler().getId().equals(playerId))
                .collect(Collectors.groupingBy(b -> b.getInnings().getId()));

        int totalWickets = 0, totalBallsBowled = 0, totalRunsConceded = 0;
        int maidens = 0, dotBalls = 0, threeWicketHauls = 0, fiveWicketHauls = 0;

        for (Map.Entry<Long, List<CricketBall>> entry : bowlingByInnings.entrySet()) {
            List<CricketBall> inningsBalls = entry.getValue();

            int inningsWickets = 0;

            // Group by over number to detect maidens
            Map<Integer, List<CricketBall>> byOver = inningsBalls.stream()
                    .filter(b -> b.getOverNumber() != null)
                    .collect(Collectors.groupingBy(CricketBall::getOverNumber));

            for (Map.Entry<Integer, List<CricketBall>> overEntry : byOver.entrySet()) {
                List<CricketBall> overBalls = overEntry.getValue();

                int overRuns = 0;
                boolean overComplete = false;
                int legalCount = 0;

                for (CricketBall b : overBalls) {
                    // Runs conceded by bowler = bat runs + wides + no-balls (NOT byes/legbyes)
                    if (!isByeOrLegBye(b)) {
                        overRuns += safeInt(b.getRuns()) + safeInt(b.getExtra());
                    }

                    if (Boolean.TRUE.equals(b.getLegalDelivery())) {
                        legalCount++;
                        totalBallsBowled++;

                        // Dot ball = legal delivery with 0 runs AND no extra
                        if (safeInt(b.getRuns()) == 0 && safeInt(b.getExtra()) == 0
                                && b.getDismissalType() == null) {
                            dotBalls++;
                        }
                    }

                    // Wicket — exclude run-outs (credit goes to fielder, not bowler)
                    if (b.getDismissalType() != null
                            && !b.getDismissalType().equalsIgnoreCase("runout")
                            && !b.getDismissalType().equalsIgnoreCase("run_out")) {
                        inningsWickets++;
                        totalWickets++;
                    }
                }

                totalRunsConceded += overRuns;

                // Maiden = complete 6-ball over with 0 runs conceded
                if (legalCount >= 6 && overRuns == 0) maidens++;
            }

            if (inningsWickets >= 5)      fiveWicketHauls++;
            else if (inningsWickets >= 3) threeWicketHauls++;
        }

        // Economy = runs conceded / overs bowled
        double overs = totalBallsBowled / 6.0;
        double economy = overs > 0 ? roundTo2(totalRunsConceded / overs) : 0.0;

        // Bowling Average = runs conceded / wickets
        double bowlingAvg = totalWickets > 0
                ? roundTo2((double) totalRunsConceded / totalWickets) : 0.0;

        // Bowling Strike Rate = balls bowled / wickets
        double bowlingSR = totalWickets > 0
                ? roundTo2((double) totalBallsBowled / totalWickets) : 0.0;

        // -------- FIELDING --------
        // Catches: fielder present + dismissalType = "caught"
        int catches = (int) allBalls.stream()
                .filter(b -> b.getFielder() != null
                        && b.getFielder().getId().equals(playerId)
                        && b.getDismissalType() != null
                        && b.getDismissalType().equalsIgnoreCase("caught"))
                .count();

        // Stumpings: fielder present + dismissalType = "stumped"
        int stumpings = (int) allBalls.stream()
                .filter(b -> b.getFielder() != null
                        && b.getFielder().getId().equals(playerId)
                        && b.getDismissalType() != null
                        && b.getDismissalType().equalsIgnoreCase("stumped"))
                .count();

        // Run-outs: fielder present + dismissalType = "runout"
        int runouts = (int) allBalls.stream()
                .filter(b -> b.getFielder() != null
                        && b.getFielder().getId().equals(playerId)
                        && b.getDismissalType() != null
                        && b.getDismissalType().toLowerCase().contains("runout"))
                .count();

        // -------- WRITE BACK --------
        stats.setRuns(totalRuns);
        stats.setBallsFaced(totalBallsFaced);
        stats.setFours(totalFours);
        stats.setSixes(totalSixes);
        stats.setHighest(highest);
        stats.setFifties(fifties);
        stats.setHundreds(hundreds);
        stats.setNotOut(notOuts);
        stats.setInningsPlayed(inningsPlayed);
        stats.setStrikeRate((int) strikeRate);
        stats.setBattingAverage(battingAvg);

        stats.setWickets(totalWickets);
        stats.setBallsBowled(totalBallsBowled);
        stats.setRunsConceded(totalRunsConceded);
        stats.setMaidens(maidens);
        stats.setDotBalls(dotBalls);
        stats.setThreeWicketHauls(threeWicketHauls);
        stats.setFiveWicketHauls(fiveWicketHauls);
        stats.setEconomy(economy);
        stats.setBowlingAverage(bowlingAvg);
        stats.setBowlingStrikeRate(bowlingSR);

        stats.setCatches(catches);
        stats.setStumpings(stumpings);
        stats.setRunouts(runouts);

        // Fantasy-style points (optional, adjust weights as needed)
        int pomCount = awardInterface.countPomByPlayerId(playerId);
        stats.setPlayerOfMatchCount(pomCount);
        stats.setPoints(calculateFantasyPoints(stats));

        statsInterface.save(stats);
    }

    // ==================== PLAYER OF THE MATCH ====================

    /**
     * Finds the best-performing player in this match using a simple scoring formula,
     * saves an Award row, and updates match.manOfMatch.
     *
     * Formula (per match):
     *   batting score  = runs + (fours * 1) + (sixes * 2) + (100 * 15) + (50 * 10)
     *   bowling score  = (wickets * 20) + (3W * 10) + (5W * 15) + (maidens * 5)
     *   fielding score = (catches * 8) + (stumpings * 10) + (runouts * 6)
     */
    @Transactional
    public void calculatePlayerOfMatch(Long matchId) {
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null) return;
        if (!awardInterface.findByMatchIdAndAwardType(matchId, "PLAYER_OF_MATCH").isEmpty()) return;
        // Collect all balls for this match
        List<CricketBall> allBalls = new ArrayList<>();
        for (CricketInnings ci : match.getCricketInnings()) {
            allBalls.addAll(cricketBallInterface.findAllByInningsAndMatch(ci.getId(), matchId));
        }

        // Per-player match score map
        Map<Long, Integer> scoreMap = new HashMap<>();
        Map<Long, Player> playerMap = new HashMap<>();

        for (CricketBall b : allBalls) {
            // --- Batting ---
            if (b.getBatsman() != null) {
                long pid = b.getBatsman().getId();
                playerMap.put(pid, b.getBatsman());
                int pts = 0;
                if (!isByeOrLegBye(b)) pts += safeInt(b.getRuns());
                if (Boolean.TRUE.equals(b.getIsFour())) pts += 1;
                if (Boolean.TRUE.equals(b.getIsSix())) pts += 2;
                scoreMap.merge(pid, pts, Integer::sum);
            }

            // --- Bowling ---
            if (b.getBowler() != null) {
                long pid = b.getBowler().getId();
                playerMap.put(pid, b.getBowler());
                int pts = 0;
                if (b.getDismissalType() != null
                        && !b.getDismissalType().equalsIgnoreCase("runout")
                        && !b.getDismissalType().equalsIgnoreCase("run_out")) {
                    pts += 20; // wicket
                }
                // Dot ball bonus
                if (Boolean.TRUE.equals(b.getLegalDelivery())
                        && safeInt(b.getRuns()) == 0 && safeInt(b.getExtra()) == 0
                        && b.getDismissalType() == null) {
                    pts += 1;
                }
                scoreMap.merge(pid, pts, Integer::sum);
            }

            // --- Fielding ---
            if (b.getFielder() != null && b.getDismissalType() != null) {
                long pid = b.getFielder().getId();
                playerMap.put(pid, b.getFielder());
                int pts = switch (b.getDismissalType().toLowerCase()) {
                    case "caught" -> 8;
                    case "stumped" -> 10;
                    default -> b.getDismissalType().toLowerCase().contains("runout") ? 6 : 0;
                };
                scoreMap.merge(pid, pts, Integer::sum);
            }
        }

        // Bonus: 50+ and 100+ (scan per-innings totals)
        Map<Long, Map<Long, Integer>> inningsRunsPerPlayer = new HashMap<>();
        for (CricketBall b : allBalls) {
            if (b.getBatsman() == null || isByeOrLegBye(b)) continue;
            long pid = b.getBatsman().getId();
            long iid = b.getInnings().getId();
            inningsRunsPerPlayer.computeIfAbsent(pid, k -> new HashMap<>())
                    .merge(iid, safeInt(b.getRuns()), Integer::sum);
        }
        for (Map.Entry<Long, Map<Long, Integer>> e : inningsRunsPerPlayer.entrySet()) {
            long pid = e.getKey();
            for (int r : e.getValue().values()) {
                if (r >= 100) scoreMap.merge(pid, 15, Integer::sum);
                else if (r >= 50) scoreMap.merge(pid, 10, Integer::sum);
            }
        }

        if (scoreMap.isEmpty()) return;

        // Best player
        Long bestPid = scoreMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        if (bestPid == null) return;

        Player bestPlayer = playerMap.get(bestPid);
        int bestScore = scoreMap.get(bestPid);

        // Save award
        Award award = new Award();
        award.setMatch(match);
        award.setTournament(match.getTournament());
        award.setPlayer(bestPlayer);
        award.setAwardType("PLAYER_OF_MATCH");
        award.setPointsEarned(bestScore);
        award.setReason("Highest match score: " + bestScore + " pts");
        awardInterface.save(award);

        // Update match record
        match.setManOfMatch(bestPlayer);
        matchRepo.save(match);

        // Update tournament stats POM count
    }

    // ==================== TOURNAMENT END AWARDS ====================

    /**
     * Called after each match. If ALL matches in tournament are COMPLETED,
     * generate: MAN_OF_TOURNAMENT, BEST_BATSMAN, BEST_BOWLER, BEST_FIELDER.
     */
    @Transactional
    public void checkAndHandleTournamentEnd(Tournament tournament) {
        if (tournament == null) return;

        // ✅ allDone check hatao — jo matches complete hain unki stats pe calculate karo

        // Purani tournament awards delete karo
        List<Award> existingAwards = awardInterface.findByTournamentId(tournament.getId());
        List<Award> tournamentAwards = existingAwards.stream()
                .filter(a -> a.getMatch() == null)
                .collect(Collectors.toList());
        awardInterface.deleteAll(tournamentAwards);

        // Stats fetch karo
        List<Stats> allStats = statsInterface.findAllByTournamentId(tournament.getId());
        if (allStats.isEmpty()) return;

        // BEST BATSMAN
        allStats.stream()
                .filter(s -> s.getRuns() > 0) // ✅ zero runners ignore
                .max(Comparator.comparingInt(Stats::getRuns)
                        .thenComparingDouble(s -> s.getBattingAverage() != null ? s.getBattingAverage() : 0))
                .ifPresent(best -> saveAward(null, tournament, best.getPlayer(),
                        "BEST_BATSMAN", best.getRuns(),
                        "Most runs: " + best.getRuns() + " @ avg " + best.getBattingAverage()));

        // BEST BOWLER
        allStats.stream()
                .filter(s -> s.getBallsBowled() > 0) // wickets > 0 ki jagah
                .min(Comparator.comparingDouble(s -> s.getEconomy() != null ? s.getEconomy() : 999)) // lowest economy
                .ifPresent(best -> saveAward(null, tournament, best.getPlayer(),
                        "BEST_BOWLER", best.getBallsBowled(),
                        "Balls: " + best.getBallsBowled() + " | Economy: " + best.getEconomy()));

        // BEST FIELDER
        allStats.stream()
                .filter(s -> (s.getCatches() + s.getStumpings() + s.getRunouts()) > 0)
                .max(Comparator.comparingInt(s -> s.getCatches() + s.getStumpings() + s.getRunouts()))
                .ifPresent(best -> {
                    int total = best.getCatches() + best.getStumpings() + best.getRunouts();
                    saveAward(null, tournament, best.getPlayer(),
                            "BEST_FIELDER", total,
                            "Catches: " + best.getCatches() + ", Stumpings: "
                                    + best.getStumpings() + ", Run-outs: " + best.getRunouts());
                });

        // MAN OF TOURNAMENT
        List<Stats> top3 = allStats.stream()
                .filter(s -> s.getPoints() != null && s.getPoints() > 0)
                .sorted(Comparator.comparingInt(Stats::getPoints).reversed())
                .limit(3)
                .collect(Collectors.toList());
        int motRank = 1;
        for (Stats best : top3) {
            saveAward(null, tournament, best.getPlayer(),
                    "MAN_OF_TOURNAMENT", best.getPoints(),
                    "Rank " + motRank++ + " - Tournament points: " + best.getPoints());
        }
    }
    // ==================== AWARD HELPER ====================

    private void saveAward(Match match, Tournament tournament, Player player,
                           String type, int points, String reason) {
        Award a = new Award();
        a.setMatch(match);
        a.setTournament(tournament);
        a.setPlayer(player);
        a.setAwardType(type);
        a.setPointsEarned(points);
        a.setReason(reason);
        awardInterface.save(a);
    }

    // ==================== FANTASY POINTS ====================

    /**
     * Optional fantasy-style points tally stored on Stats.
     * Adjust multipliers to taste.
     */
    private int calculateFantasyPoints(Stats s) {
        int pts = 0;
        pts += s.getRuns();                        // 1 pt per run
        pts += s.getFours()    * 1;
        pts += s.getSixes()    * 2;
        pts += s.getFifties()  * 10;
        pts += s.getHundreds() * 20;
        pts += s.getWickets()  * 20;
        pts += s.getMaidens()  * 5;
        pts += s.getDotBalls() * 1;
        pts += s.getThreeWicketHauls() * 10;
        pts += s.getFiveWicketHauls()  * 20;
        pts += s.getCatches()   * 8;
        pts += s.getStumpings() * 10;
        pts += s.getRunouts()   * 6;
        pts += s.getPlayerOfMatchCount() * 25;
        return pts;
    }

    // ==================== PLAYER FULL STATS DTO ====================

// ─── Replace getPlayerFullStats() in StatsService.java ────────────────────────

// ─── StatsService.java — getPlayerFullStats() replace karo ───────
// Root cause: matchesPlayed har sport ke liye alag honi chahiye.
// findMatchesByTeam() all sports count karta tha.

    public PlayerFullStatsDTO getPlayerFullStats(Long playerId, Long tournamentId, String requestedSport) {
        PlayerFullStatsDTO dto = new PlayerFullStatsDTO();

        // ── Fetch Stats row ───────────────────────────────────────────
        Stats s;
        if (tournamentId != null) {
            s = statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).orElse(null);
        } else if (requestedSport != null) {
            // Overall view — sport chip se aaya
            List<Stats> sportStats = statsInterface.findByPlayerIdAndSport(playerId, requestedSport);
            s = sportStats.isEmpty() ? null : aggregateStats(sportStats, requestedSport);
            // Aggregated row ko player/sportType manually set karna hoga
            if (s != null && s.getPlayer() == null) {
                playerInterface.findActiveById(playerId).ifPresent(s::setPlayer);
            }
        } else {
            List<Stats> allPlayerStats = statsInterface.findByPlayerId(playerId);
            if (allPlayerStats.isEmpty()) {
                s = null;
            } else if (allPlayerStats.size() == 1) {
                s = allPlayerStats.get(0);
            } else {
                // Determine dominant sport from the list, then aggregate
                String dominantSport = allPlayerStats.get(0).getSportType() != null
                        ? allPlayerStats.get(0).getSportType().getName()
                        : "cricket";
                s = aggregateStats(allPlayerStats, dominantSport);
                if (s.getPlayer() == null) {
                    playerInterface.findActiveById(playerId).ifPresent(s::setPlayer);
                }
            }
        }

        if (s == null) {
            // Koi stats nahi — sirf sport field set karo
            String fallbackSport = requestedSport != null ? requestedSport.toLowerCase()
                    : tournamentId != null
                    ? tournamentInterface.findById(tournamentId)
                    .map(t -> t.getSport() != null ? t.getSport().getName().toLowerCase() : "cricket")
                    .orElse("cricket")
                    : "cricket";
            dto.setSport(fallbackSport);
            setZeroMatchCounts(dto, playerId);
            return dto;
        }

        dto.setPlayerId(playerId);
        if (s.getPlayer() != null) dto.setPlayerName(s.getPlayer().getName());

        // ── Sport identifier ─────────────────────────────────────────
        String sport = requestedSport != null ? requestedSport.toLowerCase()
                : (s.getSportType() != null && s.getSportType().getName() != null
                ? s.getSportType().getName().toLowerCase()
                : "cricket");
        dto.setSport(sport);

        // ── ✅ Sport-specific match counts — ONLY the selected sport ──
        setMatchCounts(dto, playerId, sport);

        // ── POM — sport-specific ─────────────────────────────────────
        dto.setPomCount(awardInterface.countPomByPlayerIdAndSport(playerId, sport));

        // ── Cricket stats ─────────────────────────────────────────────
        dto.setTotalRuns(safeInt(s.getRuns()));
        dto.setBallsFaced(safeInt(s.getBallsFaced()));
        dto.setStrikeRate(s.getStrikeRate() != null ? s.getStrikeRate() : 0);
        dto.setBattingAvg(s.getBattingAverage() != null ? s.getBattingAverage() : 0.0);
        dto.setHighest(safeInt(s.getHighest()));
        dto.setFours(safeInt(s.getFours()));
        dto.setSixes(safeInt(s.getSixes()));
        dto.setNotOuts(safeInt(s.getNotOut()));
        dto.setFifties(safeInt(s.getFifties()));
        dto.setHundreds(safeInt(s.getHundreds()));
        dto.setWickets(safeInt(s.getWickets()));
        dto.setBallsBowled(safeInt(s.getBallsBowled()));
        dto.setRunsConceded(safeInt(s.getRunsConceded()));
        dto.setEconomy(s.getEconomy() != null ? s.getEconomy() : 0.0);
        dto.setBowlingAverage(s.getBowlingAverage() != null ? s.getBowlingAverage() : 0.0);
        dto.setBowlingStrikeRate(s.getBowlingStrikeRate() != null ? s.getBowlingStrikeRate() : 0.0);
        dto.setMaidens(safeInt(s.getMaidens()));
        dto.setDotBalls(safeInt(s.getDotBalls()));
        dto.setThreeWicketHauls(safeInt(s.getThreeWicketHauls()));
        dto.setFiveWicketHauls(safeInt(s.getFiveWicketHauls()));
        dto.setCatches(safeInt(s.getCatches()));
        dto.setStumpings(safeInt(s.getStumpings()));
        dto.setRunouts(safeInt(s.getRunouts()));

        // ── Multi-sport stats (goals/assists/fouls reused) ────────────
        dto.setGoals(safeInt(s.getGoals()));
        dto.setAssists(safeInt(s.getAssists()));
        dto.setFutsalFouls(safeInt(s.getFouls()));
        dto.setYellowCards(safeInt(s.getYellowCards()));
        dto.setRedCards(safeInt(s.getRedCards()));

        return dto;
    }

    private void setMatchCounts(PlayerFullStatsDTO dto, Long playerId, String sport) {
        int cricket     = matchRepo.findCricketMatchesByPlayer(playerId);
        int futsal      = matchRepo.findFutsalMatchesByPlayer(playerId);
        int volleyball  = matchRepo.findVolleyballMatchesByPlayer(playerId);
        int badminton   = matchRepo.findBadmintonMatchesByPlayer(playerId);
        int tabletennis = matchRepo.findTableTennisMatchesByPlayer(playerId);
        int tugofwar    = matchRepo.findTableTugOfWarMatchesByPlayer(playerId);
        int ludo       = matchRepo.findLudoMatchesByPlayer(playerId);
        int chess      = matchRepo.findChessMatchesByPlayer(playerId);

        dto.setCricketMatchesPlayed(cricket);
        dto.setFutsalMatchesPlayed(futsal);
        dto.setVolleyballMatchesPlayed(volleyball);
        dto.setBadmintonMatchesPlayed(badminton);
        dto.setTableTennisMatchesPlayed(tabletennis);
        dto.setTugOfWarMatchesPlayed(tugofwar);
        dto.setLudoMatchesPlayed(ludo);
        dto.setChessMatchesPlayed(chess);
        dto.setMatchesPlayed(switch (sport.toLowerCase().trim()) {
            case "futsal"                    -> futsal;
            case "volleyball"                -> volleyball;
            case "badminton"                 -> badminton;
            case "table tennis", "tabletennis" -> tabletennis;
            case "tug of war", "tugofwar"    -> tugofwar;
            case "ludo"                       -> ludo;
            case "chess"                      -> chess;
            default                          -> cricket;
        });
    }

    private void setZeroMatchCounts(PlayerFullStatsDTO dto, Long playerId) {
        setMatchCounts(dto, playerId, dto.getSport() != null ? dto.getSport() : "cricket");
    }
    // ── Aggregate multiple Stats rows into one ─────────────────────────
    private Stats aggregateStats(List<Stats> list, String sport) {
        Stats agg = new Stats();
        // Set sport so POM count works
        if (!list.isEmpty()) {
            agg.setPlayer(list.get(0).getPlayer());
            agg.setSportType(list.get(0).getSportType());
        }
        for (Stats s : list) {
            agg.setRuns(safe(agg.getRuns())        + safe(s.getRuns()));
            agg.setBallsFaced(safe(agg.getBallsFaced()) + safe(s.getBallsFaced()));
            agg.setFours(safe(agg.getFours())      + safe(s.getFours()));
            agg.setSixes(safe(agg.getSixes())      + safe(s.getSixes()));
            agg.setFifties(safe(agg.getFifties())  + safe(s.getFifties()));
            agg.setHundreds(safe(agg.getHundreds())+ safe(s.getHundreds()));
            agg.setNotOut(safe(agg.getNotOut())    + safe(s.getNotOut()));
            agg.setInningsPlayed(safe(agg.getInningsPlayed()) + safe(s.getInningsPlayed()));
            if (safe(s.getHighest()) > safe(agg.getHighest())) agg.setHighest(s.getHighest());

            agg.setWickets(safe(agg.getWickets())  + safe(s.getWickets()));
            agg.setBallsBowled(safe(agg.getBallsBowled()) + safe(s.getBallsBowled()));
            agg.setRunsConceded(safe(agg.getRunsConceded()) + safe(s.getRunsConceded()));
            agg.setMaidens(safe(agg.getMaidens())  + safe(s.getMaidens()));
            agg.setDotBalls(safe(agg.getDotBalls())+ safe(s.getDotBalls()));
            agg.setThreeWicketHauls(safe(agg.getThreeWicketHauls()) + safe(s.getThreeWicketHauls()));
            agg.setFiveWicketHauls(safe(agg.getFiveWicketHauls())   + safe(s.getFiveWicketHauls()));
            agg.setCatches(safe(agg.getCatches())  + safe(s.getCatches()));
            agg.setStumpings(safe(agg.getStumpings()) + safe(s.getStumpings()));
            agg.setRunouts(safe(agg.getRunouts())  + safe(s.getRunouts()));

            agg.setGoals(safe(agg.getGoals())      + safe(s.getGoals()));
            agg.setAssists(safe(agg.getAssists())  + safe(s.getAssists()));
            agg.setFouls(safe(agg.getFouls())      + safe(s.getFouls()));
            agg.setYellowCards(safe(agg.getYellowCards()) + safe(s.getYellowCards()));
            agg.setRedCards(safe(agg.getRedCards())+ safe(s.getRedCards()));
        }

        // Recalculate derived stats
        int tb = safe(agg.getBallsFaced());
        agg.setStrikeRate(tb > 0 ? (int)((double) safe(agg.getRuns()) / tb * 100) : 0);
        int dismissals = safe(agg.getInningsPlayed()) - safe(agg.getNotOut());
        agg.setBattingAverage(dismissals > 0 ? roundTo2((double) safe(agg.getRuns()) / dismissals) : 0.0);
        double overs = safe(agg.getBallsBowled()) / 6.0;
        agg.setEconomy(overs > 0 ? roundTo2(safe(agg.getRunsConceded()) / overs) : 0.0);
        int wkts = safe(agg.getWickets());
        agg.setBowlingAverage(wkts > 0 ? roundTo2((double) safe(agg.getRunsConceded()) / wkts) : 0.0);
        agg.setBowlingStrikeRate(wkts > 0 ? roundTo2((double) safe(agg.getBallsBowled()) / wkts) : 0.0);

        return agg;
    }

    // ── Build DTO from a Stats object ──────────────────────────────────
    private PlayerFullStatsDTO buildDTO(PlayerFullStatsDTO dto, Stats s,
                                        Long playerId, boolean usesSportPom) {
        if (s.getPlayer() != null) {
            dto.setPlayerId(s.getPlayer().getId());
            dto.setPlayerName(s.getPlayer().getName());
        }

        String sport = s.getSportType() != null && s.getSportType().getName() != null
                ? s.getSportType().getName().toLowerCase() : "cricket";
        dto.setSport(sport);






        int cricketMatches    = matchRepo.findCricketMatchesByPlayer(playerId);
        int futsalMatches     = matchRepo.findFutsalMatchesByPlayer(playerId);
        int volleyballMatches = matchRepo.findVolleyballMatchesByPlayer(playerId); // ✅ NEW
        int badmintonMatches  = matchRepo.findBadmintonMatchesByPlayer(playerId);

        dto.setCricketMatchesPlayed(cricketMatches);
        dto.setFutsalMatchesPlayed(futsalMatches);
        dto.setVolleyballMatchesPlayed(volleyballMatches); // ✅ NEW
        dto.setBadmintonMatchesPlayed(badmintonMatches);

        // ✅ matchesPlayed = current sport ka count
        dto.setMatchesPlayed(switch (sport) {
            case "futsal"     -> futsalMatches;
            case "volleyball" -> volleyballMatches;
            case "badminton"  -> badmintonMatches;
            default           -> cricketMatches;
        });

        // ✅ Sport-wise POM count
        int pomCount = awardInterface.countPomByPlayerIdAndSport(playerId, sport);
        dto.setPomCount(pomCount);

        // Cricket fields
        dto.setTotalRuns(safeInt(s.getRuns()));
        dto.setBallsFaced(safeInt(s.getBallsFaced()));
        dto.setStrikeRate(s.getStrikeRate() != null ? s.getStrikeRate() : 0);
        dto.setBattingAvg(s.getBattingAverage() != null ? s.getBattingAverage() : 0.0);
        dto.setHighest(safeInt(s.getHighest()));
        dto.setFours(safeInt(s.getFours()));
        dto.setSixes(safeInt(s.getSixes()));
        dto.setNotOuts(safeInt(s.getNotOut()));
        dto.setFifties(safeInt(s.getFifties()));
        dto.setHundreds(safeInt(s.getHundreds()));
        dto.setWickets(safeInt(s.getWickets()));
        dto.setBallsBowled(safeInt(s.getBallsBowled()));
        dto.setRunsConceded(safeInt(s.getRunsConceded()));
        dto.setEconomy(s.getEconomy() != null ? s.getEconomy() : 0.0);
        dto.setBowlingAverage(s.getBowlingAverage() != null ? s.getBowlingAverage() : 0.0);
        dto.setBowlingStrikeRate(s.getBowlingStrikeRate() != null ? s.getBowlingStrikeRate() : 0.0);
        dto.setMaidens(safeInt(s.getMaidens()));
        dto.setDotBalls(safeInt(s.getDotBalls()));
        dto.setThreeWicketHauls(safeInt(s.getThreeWicketHauls()));
        dto.setFiveWicketHauls(safeInt(s.getFiveWicketHauls()));
        dto.setCatches(safeInt(s.getCatches()));
        dto.setStumpings(safeInt(s.getStumpings()));
        dto.setRunouts(safeInt(s.getRunouts()));

        // Futsal / Volleyball fields
        dto.setGoals(safeInt(s.getGoals()));
        dto.setAssists(safeInt(s.getAssists()));
        dto.setFutsalFouls(safeInt(s.getFouls()));
        dto.setYellowCards(safeInt(s.getYellowCards()));
        dto.setRedCards(safeInt(s.getRedCards()));

        return dto;
    }

    private int safe(Integer v) { return v == null ? 0 : v; }
// safeInt helper (already exists in your StatsService, no need to add again)
// private int safeInt(Integer value) { return value != null ? value : 0; }

    // ==================== HELPERS ====================

    private boolean isWide(CricketBall cb) {
        return cb.getExtraType() != null && cb.getExtraType().equalsIgnoreCase("wide");
    }

    private boolean isByeOrLegBye(CricketBall cb) {
        if (cb.getExtraType() == null) return false;
        String et = cb.getExtraType().toLowerCase();
        return et.equals("bye") || et.equals("legbye");
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private double roundTo2(double val) {
        if (Double.isInfinite(val) || Double.isNaN(val)) return 0.0;
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
