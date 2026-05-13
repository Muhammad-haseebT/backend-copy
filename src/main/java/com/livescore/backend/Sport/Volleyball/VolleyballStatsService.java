package com.livescore.backend.Sport.Volleyball;

import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Volleyball.VolleyballEvent;
import com.livescore.backend.Interface.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolleyballStatsService {

    private final StatsInterface statsInterface;
    private final PlayerInterface playerInterface;
    private final TournamentInterface tournamentInterface;
    private final AwardInterface awardInterface;
    private final MatchInterface matchInterface;
    private final VolleyballEventInterface volleyballEventInterface;

    @Transactional
    public void onEventSaved(VolleyballEvent event) {
        if (event == null || event.getMatch() == null || event.getMatch().getTournament() == null) return;
        Long tournamentId = event.getMatch().getTournament().getId();

        Set<Long> playerIds = new HashSet<>();
        if (event.getPlayer() != null) playerIds.add(event.getPlayer().getId());

        for (Long pid : playerIds) {
            ensureStats(pid, tournamentId, event.getMatch().getTournament());
            recalculatePlayerStats(pid, tournamentId);
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "vbStates", allEntries = true, beforeInvocation = false)

    })
    public void onMatchEnd(Long matchId) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null) return;

        Long tournamentId = match.getTournament().getId();
        List<VolleyballEvent> events = volleyballEventInterface.findByMatch_IdOrderByIdAsc(matchId);

        Set<Long> playerIds = events.stream()
                .filter(e -> e.getPlayer() != null)
                .map(e -> e.getPlayer().getId())
                .collect(Collectors.toSet());

        for (Long pid : playerIds) {
            ensureStats(pid, tournamentId, match.getTournament());
            recalculatePlayerStats(pid, tournamentId);
        }

        calculatePlayerOfMatch(matchId, events, match);
        updateTournamentAwards(match.getTournament());
    }

    @Transactional
    public void recalculatePlayerStats(Long playerId, Long tournamentId) {
        Stats stats = statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).orElse(null);
        if (stats == null) return;

        List<VolleyballEvent> events = volleyballEventInterface
                .findByPlayerIdAndTournamentId(playerId, tournamentId);

        int points = 0, aces = 0, blocks = 0, attackErrors = 0, serviceErrors = 0;

        for (VolleyballEvent ev : events) {
            if (ev.getPlayer() == null || !ev.getPlayer().getId().equals(playerId)) continue;
            switch (ev.getEventType().toUpperCase()) {
                case "POINT":         points++;       break;
                case "ACE":           aces++;  points++; break;
                case "BLOCK":         blocks++; points++; break;
                case "ATTACK_ERROR":  attackErrors++;  break;
                case "SERVICE_ERROR": serviceErrors++; break;
            }
        }

        // Store in Stats entity futsal/football fields repurposed:
        // goals = volleyball points scored
        // assists = aces
        // fouls = blocks
        // yellowCards = attack errors
        // redCards = service errors
        stats.setGoals(points);
        stats.setAssists(aces);
        stats.setFouls(blocks);
        stats.setYellowCards(attackErrors);
        stats.setRedCards(serviceErrors);

        int pomCount = awardInterface.countPomByPlayerIdAndSport(playerId, "volleyball");
        stats.setPlayerOfMatchCount(pomCount);
        stats.setPoints(calculateFantasyPoints(points, aces, blocks, attackErrors, serviceErrors, pomCount));

        statsInterface.save(stats);
    }

    @Transactional
    public void calculatePlayerOfMatch(Long matchId, List<VolleyballEvent> events, Match match) {
        if (!awardInterface.findByMatchIdAndAwardType(matchId, "PLAYER_OF_MATCH").isEmpty()) return;

        Map<Long, Integer> scoreMap = new HashMap<>();
        Map<Long, Player>  playerMap = new HashMap<>();

        for (VolleyballEvent ev : events) {
            if (ev.getPlayer() == null) continue;
            Long pid = ev.getPlayer().getId();
            playerMap.put(pid, ev.getPlayer());
            int pts = switch (ev.getEventType().toUpperCase()) {
                case "ACE"           -> 5;
                case "BLOCK"         -> 4;
                case "POINT"         -> 2;
                case "ATTACK_ERROR"  -> -2;
                case "SERVICE_ERROR" -> -1;
                default              -> 0;
            };
            scoreMap.merge(pid, pts, Integer::sum);
        }

        if (scoreMap.isEmpty()) return;

        Long bestPid = scoreMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
        if (bestPid == null) return;

        Award award = new Award();
        award.setMatch(match);
        award.setTournament(match.getTournament());
        award.setPlayer(playerMap.get(bestPid));
        award.setAwardType("PLAYER_OF_MATCH");
        award.setPointsEarned(scoreMap.get(bestPid));
        award.setReason("Best volleyball performance: " + scoreMap.get(bestPid) + " pts");
        awardInterface.save(award);
        match.setStatus("COMPLETED");
        match.setManOfMatch(playerMap.get(bestPid));
        matchInterface.save(match);
    }

    @Transactional
    public void updateTournamentAwards(Tournament tournament) {
        awardInterface.findByTournamentId(tournament.getId()).stream()
                .filter(a -> a.getMatch() == null)
                .forEach(awardInterface::delete);

        List<Stats> allStats = statsInterface.findAllByTournamentId(tournament.getId());
        if (allStats.isEmpty()) return;

        // TOP SCORER (most points)
        allStats.stream()
                .filter(s -> s.getGoals() != null && s.getGoals() > 0)
                .max(Comparator.comparingInt(Stats::getGoals))
                .ifPresent(s -> saveAward(null, tournament, s.getPlayer(),
                        "TOP_SCORER", s.getGoals(), "Most points: " + s.getGoals()));

        // BEST SERVER (most aces)
        allStats.stream()
                .filter(s -> s.getAssists() != null && s.getAssists() > 0)
                .max(Comparator.comparingInt(Stats::getAssists))
                .ifPresent(s -> saveAward(null, tournament, s.getPlayer(),
                        "BEST_SERVER", s.getAssists(), "Most aces: " + s.getAssists()));

        // BEST BLOCKER (most blocks)
        allStats.stream()
                .filter(s -> s.getFouls() != null && s.getFouls() > 0)
                .max(Comparator.comparingInt(Stats::getFouls))
                .ifPresent(s -> saveAward(null, tournament, s.getPlayer(),
                        "BEST_BLOCKER", s.getFouls(), "Most blocks: " + s.getFouls()));

        // MAN OF TOURNAMENT
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

    private void ensureStats(Long playerId, Long tournamentId, Tournament tournament) {
        if (statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).isPresent()) return;
        Player player = playerInterface.findActiveById(playerId).orElse(null);
        if (player == null) return;
        Stats s = new Stats();
        s.setPlayer(player); s.setTournament(tournament);
        s.setSportType(tournament.getSport());
        s.setGoals(0); s.setAssists(0); s.setFouls(0);
        s.setYellowCards(0); s.setRedCards(0); s.setPoints(0);
        s.setRuns(0); s.setWickets(0); s.setBallsFaced(0); s.setBallsBowled(0);
        s.setRunsConceded(0); s.setFours(0); s.setSixes(0); s.setFifties(0);
        s.setHundreds(0); s.setHighest(0); s.setNotOut(0); s.setInningsPlayed(0);
        s.setMaidens(0); s.setThreeWicketHauls(0); s.setFiveWicketHauls(0);
        s.setDotBalls(0); s.setCatches(0); s.setRunouts(0); s.setStumpings(0);
        s.setPlayerOfMatchCount(0); s.setStrikeRate(0);
        s.setBattingAverage(0.0); s.setEconomy(0.0);
        s.setBowlingAverage(0.0); s.setBowlingStrikeRate(0.0);
        statsInterface.save(s);
    }

    private int calculateFantasyPoints(int points, int aces, int blocks,
                                        int attackErrors, int serviceErrors, int pomCount) {
        return (points * 2) + (aces * 5) + (blocks * 4)
                - (attackErrors * 2) - serviceErrors
                + (pomCount * 25);
    }

    private void saveAward(Match match, Tournament tournament, Player player,
                           String type, int pts, String reason) {
        Award a = new Award();
        a.setMatch(match); a.setTournament(tournament); a.setPlayer(player);
        a.setAwardType(type); a.setPointsEarned(pts); a.setReason(reason);
        awardInterface.save(a);
    }
}
