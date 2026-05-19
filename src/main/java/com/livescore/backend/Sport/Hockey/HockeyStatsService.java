package com.livescore.backend.Sport.Hockey;

import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Hockey.HockeyEvent;
import com.livescore.backend.Entity.Hockey.HockeyMatchStats;
import com.livescore.backend.Interface.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HockeyStatsService {

    private final StatsInterface statsInterface;
    private final PlayerInterface playerInterface;
    private final TournamentInterface tournamentInterface;
    private final AwardInterface awardInterface;
    private final MatchInterface matchInterface;
    private final HockeyEventInterface hockeyEventInterface;
    private final HockeyMatchStatsInterface hockeyMatchStatsInterface;

    @Autowired
    private HockeyPtsTableService hockeyPtsTableService;

    // ─────────────────────────────────────────────
    // Called after EVERY hockey event
    // ─────────────────────────────────────────────

    @Transactional
    public void onEventSaved(HockeyEvent event) {
        if (event == null || event.getMatch() == null) return;

        Match match = event.getMatch();
        if (match.getTournament() == null) return;

        Long tournamentId = match.getTournament().getId();

        Set<Long> playerIds = new HashSet<>();
        if (event.getPlayer() != null)       playerIds.add(event.getPlayer().getId());
        if (event.getAssistPlayer() != null) playerIds.add(event.getAssistPlayer().getId());

        for (Long pid : playerIds) {
            ensureTournamentStats(pid, tournamentId, match.getTournament());
            recalculatePlayerTournamentStats(pid, tournamentId);
            recalculatePlayerMatchStats(pid, match);
        }
    }

    // ─────────────────────────────────────────────
    // Called when match ends — POM, tournament awards
    // ─────────────────────────────────────────────

    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
    @Transactional
    public void onMatchEnd(Long matchId, int ts1, int ts2) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null) return;

        Long tournamentId = match.getTournament().getId();

        List<HockeyEvent> events = hockeyEventInterface.findByMatch_IdOrderByIdAsc(matchId);
        Set<Long> playerIds = new HashSet<>();
        for (HockeyEvent ev : events) {
            if (ev.getPlayer() != null)       playerIds.add(ev.getPlayer().getId());
            if (ev.getAssistPlayer() != null) playerIds.add(ev.getAssistPlayer().getId());
        }

        for (Long pid : playerIds) {
            ensureTournamentStats(pid, tournamentId, match.getTournament());
            recalculatePlayerTournamentStats(pid, tournamentId);
            recalculatePlayerMatchStats(pid, match);
        }

        calculatePlayerOfMatch(matchId, events, match);
        updateTournamentAwards(match.getTournament());
        hockeyPtsTableService.updateAfterMatch(matchId);
        hockeyPtsTableService.updateGoalData(matchId, ts1, ts2);
    }

    // ─────────────────────────────────────────────
    // TOURNAMENT-LEVEL STATS (across all matches)
    // ─────────────────────────────────────────────

    @Transactional
    public void recalculatePlayerTournamentStats(Long playerId, Long tournamentId) {
        Stats stats = statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId)
                .orElse(null);
        if (stats == null) return;

        List<HockeyEvent> events = hockeyEventInterface
                .findByPlayerIdAndTournamentId(playerId, tournamentId);

        int goals = 0, ownGoals = 0, assists = 0;
        int fouls = 0, greenCards = 0, yellowCards = 0, redCards = 0, penaltyCorners = 0;

        for (HockeyEvent ev : events) {
            String type = ev.getEventType().toUpperCase();
            boolean isMainPlayer = ev.getPlayer() != null && ev.getPlayer().getId().equals(playerId);
            boolean isAssist     = ev.getAssistPlayer() != null && ev.getAssistPlayer().getId().equals(playerId);

            if (isMainPlayer) {
                switch (type) {
                    case "GOAL":            goals++;                        break;
                    case "OWN_GOAL":        ownGoals++;                     break;
                    case "FOUL":            fouls++;                        break;
                    case "GREEN_CARD":      greenCards++; fouls++;          break;
                    case "YELLOW_CARD":     yellowCards++; fouls++;         break;
                    case "RED_CARD":        redCards++;                     break;
                    case "PENALTY_CORNER":  penaltyCorners++;               break;
                    default:                                                break;
                }
            }
            if (isAssist) assists++;
        }

        stats.setGoals(goals);
        stats.setAssists(assists);
        stats.setFouls(fouls);
        stats.setYellowCards(yellowCards);
        stats.setRedCards(redCards);

        int pomCount = awardInterface.countPomByPlayerId(playerId);
        stats.setPlayerOfMatchCount(pomCount);
        // Fantasy points: goals×15 + assists×8 + penaltyCorners×3 - greenCards×1 - yellowCards×3 - redCards×10 + pom×20
        stats.setPoints(calculateFantasyPoints(goals, assists, penaltyCorners, greenCards, yellowCards, redCards, pomCount));

        statsInterface.save(stats);
    }

    // ─────────────────────────────────────────────
    // PER-MATCH STATS
    // ─────────────────────────────────────────────

    @Transactional
    public void recalculatePlayerMatchStats(Long playerId, Match match) {
        HockeyMatchStats ms = hockeyMatchStatsInterface
                .findByMatch_IdAndPlayer_Id(match.getId(), playerId)
                .orElseGet(() -> {
                    HockeyMatchStats newMs = new HockeyMatchStats();
                    newMs.setMatch(match);
                    Player p = playerInterface.findActiveById(playerId).orElse(null);
                    newMs.setPlayer(p);
                    // Try to find team from events
                    List<HockeyEvent> ev = hockeyEventInterface
                            .findByMatch_IdOrderByIdAsc(match.getId());
                    ev.stream()
                            .filter(e -> e.getPlayer() != null && e.getPlayer().getId().equals(playerId))
                            .findFirst()
                            .ifPresent(e -> newMs.setTeam(e.getTeam()));
                    return newMs;
                });

        // Recalculate from events in THIS match only
        List<HockeyEvent> matchEvents = hockeyEventInterface
                .findByMatch_IdOrderByIdAsc(match.getId())
                .stream()
                .filter(e ->
                        (e.getPlayer() != null && e.getPlayer().getId().equals(playerId)) ||
                                (e.getAssistPlayer() != null && e.getAssistPlayer().getId().equals(playerId))
                )
                .collect(Collectors.toList());

        int goals = 0, ownGoals = 0, assists = 0;
        int fouls = 0, greenCards = 0, yellowCards = 0, redCards = 0, penaltyCorners = 0;

        for (HockeyEvent ev : matchEvents) {
            String type = ev.getEventType().toUpperCase();
            boolean isMain   = ev.getPlayer() != null && ev.getPlayer().getId().equals(playerId);
            boolean isAssist = ev.getAssistPlayer() != null && ev.getAssistPlayer().getId().equals(playerId);

            if (isMain) {
                switch (type) {
                    case "GOAL":            goals++;                        break;
                    case "OWN_GOAL":        ownGoals++;                     break;
                    case "FOUL":            fouls++;                        break;
                    case "GREEN_CARD":      greenCards++; fouls++;          break;
                    case "YELLOW_CARD":     yellowCards++; fouls++;         break;
                    case "RED_CARD":        redCards++;                     break;
                    case "PENALTY_CORNER":  penaltyCorners++;               break;
                    default:                                                break;
                }
            }
            if (isAssist) assists++;
        }

        ms.setGoals(goals);
        ms.setOwnGoals(ownGoals);
        ms.setAssists(assists);
        ms.setFouls(fouls);
        ms.setGreenCards(greenCards);
        ms.setYellowCards(yellowCards);
        ms.setRedCards(redCards);
        ms.setPenaltyCorners(penaltyCorners);
        ms.setMatchPoints(calculateFantasyPoints(goals, assists, penaltyCorners, greenCards, yellowCards, redCards, 0));

        hockeyMatchStatsInterface.save(ms);
    }

    // ─────────────────────────────────────────────
    // PLAYER OF THE MATCH
    // ─────────────────────────────────────────────

    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
    @Transactional
    public void calculatePlayerOfMatch(Long matchId, List<HockeyEvent> events, Match match) {
        if (!awardInterface.findByMatchIdAndAwardType(matchId, "PLAYER_OF_MATCH").isEmpty()) return;

        // goal=15, assist=8, penaltyCorner=3, ownGoal=-5, foul=-1, greenCard=-1, yellow=-3, red=-10
        Map<Long, Integer> scoreMap  = new HashMap<>();
        Map<Long, Player>  playerMap = new HashMap<>();

        for (HockeyEvent ev : events) {
            if (ev.getPlayer() != null) {
                Long pid = ev.getPlayer().getId();
                playerMap.put(pid, ev.getPlayer());
                int pts = switch (ev.getEventType().toUpperCase()) {
                    case "GOAL"           -> 15;
                    case "OWN_GOAL"       -> -5;
                    case "FOUL"           -> -1;
                    case "GREEN_CARD"     -> -1;
                    case "YELLOW_CARD"    -> -3;
                    case "RED_CARD"       -> -10;
                    case "PENALTY_CORNER" -> 3;
                    default               -> 0;
                };
                scoreMap.merge(pid, pts, Integer::sum);
            }
            if (ev.getAssistPlayer() != null) {
                Long aid = ev.getAssistPlayer().getId();
                playerMap.put(aid, ev.getAssistPlayer());
                scoreMap.merge(aid, 8, Integer::sum);
            }
        }

        if (scoreMap.isEmpty()) return;

        Long bestPid = scoreMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
        if (bestPid == null) return;

        Player best      = playerMap.get(bestPid);
        int    bestScore = scoreMap.get(bestPid);

        // Save award
        Award award = new Award();
        award.setMatch(match);
        award.setTournament(match.getTournament());
        award.setPlayer(best);
        award.setAwardType("PLAYER_OF_MATCH");
        award.setPointsEarned(bestScore);
        award.setReason("Best hockey performance: " + bestScore + " pts");
        awardInterface.save(award);

        match.setStatus("COMPLETED");
        match.setManOfMatch(best);
        matchInterface.save(match);

        // Mark isPom on match stats
        hockeyMatchStatsInterface.findByMatch_IdAndPlayer_Id(match.getId(), bestPid)
                .ifPresent(ms -> {
                    ms.setIsPom(true);
                    hockeyMatchStatsInterface.save(ms);
                });
    }

    // ─────────────────────────────────────────────
    // TOURNAMENT AWARDS
    // ─────────────────────────────────────────────

    @Transactional
    public void updateTournamentAwards(Tournament tournament) {
        // Clear old tournament-level awards
        awardInterface.findByTournamentId(tournament.getId()).stream()
                .filter(a -> a.getMatch() == null)
                .forEach(awardInterface::delete);

        List<Stats> allStats = statsInterface.findAllByTournamentId(tournament.getId());
        if (allStats.isEmpty()) return;

        // TOP SCORER
        allStats.stream()
                .filter(s -> s.getGoals() != null && s.getGoals() > 0)
                .max(Comparator.comparingInt(Stats::getGoals))
                .ifPresent(s -> saveAward(null, tournament, s.getPlayer(),
                        "TOP_SCORER", s.getGoals(), "Most goals: " + s.getGoals()));

        // TOP ASSIST
        allStats.stream()
                .filter(s -> s.getAssists() != null && s.getAssists() > 0)
                .max(Comparator.comparingInt(Stats::getAssists))
                .ifPresent(s -> saveAward(null, tournament, s.getPlayer(),
                        "TOP_ASSIST", s.getAssists(), "Most assists: " + s.getAssists()));

        // MAN OF TOURNAMENT (highest fantasy points)
        List<Stats> top3 = allStats.stream()
                .filter(s -> s.getPoints() != null && s.getPoints() > 0)
                .sorted(Comparator.comparingInt(Stats::getPoints).reversed())
                .limit(3)
                .collect(Collectors.toList());
        int motRank = 1;
        for (Stats s : top3) {
            saveAward(null, tournament, s.getPlayer(),
                    "MAN_OF_TOURNAMENT", s.getPoints(),
                    "Rank " + motRank++ + " - Tournament points: " + s.getPoints());
        }
    }

    // ─────────────────────────────────────────────
    // GET match-by-match progress for a player
    // ─────────────────────────────────────────────

    public List<HockeyMatchStats> getPlayerMatchHistory(Long playerId, Long tournamentId) {
        return hockeyMatchStatsInterface.findByPlayerAndTournament(playerId, tournamentId);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private void ensureTournamentStats(Long playerId, Long tournamentId, Tournament tournament) {
        if (statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).isPresent()) return;

        Player player = playerInterface.findActiveById(playerId).orElse(null);
        if (player == null) return;

        Stats s = new Stats();
        s.setPlayer(player);
        s.setTournament(tournament);
        s.setSportType(tournament.getSport());
        s.setGoals(0);   s.setAssists(0);  s.setFouls(0);
        s.setYellowCards(0); s.setRedCards(0); s.setPoints(0);
        s.setRuns(0);    s.setWickets(0);  s.setBallsFaced(0);
        s.setBallsBowled(0); s.setRunsConceded(0); s.setFours(0);
        s.setSixes(0);   s.setFifties(0);  s.setHundreds(0);
        s.setHighest(0); s.setNotOut(0);   s.setInningsPlayed(0);
        s.setMaidens(0); s.setThreeWicketHauls(0); s.setFiveWicketHauls(0);
        s.setDotBalls(0); s.setCatches(0); s.setRunouts(0); s.setStumpings(0);
        s.setPlayerOfMatchCount(0); s.setStrikeRate(0);
        s.setBattingAverage(0.0); s.setEconomy(0.0);
        s.setBowlingAverage(0.0); s.setBowlingStrikeRate(0.0);
        statsInterface.save(s);
    }

    /**
     * Fantasy points formula:
     * goals×15 + assists×8 + penaltyCorners×3
     * - greenCards×1 - yellowCards×3 - redCards×10 + pomCount×20
     */
    private int calculateFantasyPoints(int goals, int assists, int penaltyCorners,
                                       int greenCards, int yellowCards, int redCards, int pomCount) {
        return (goals * 15) + (assists * 8) + (penaltyCorners * 3)
                - (greenCards * 1) - (yellowCards * 3) - (redCards * 10)
                + (pomCount * 20);
    }

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
}
