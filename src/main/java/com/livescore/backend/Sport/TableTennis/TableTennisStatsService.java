// ══ TableTennisStatsService.java ══════════════════════════════════
// Stats mapping: goals=points, assists=smashes+aces+edges, fouls=faults, yellowCards=outs
package com.livescore.backend.Sport.TableTennis;

import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.TableTennis.TableTennisEvent;
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
public class TableTennisStatsService {

    private final StatsInterface        statsInterface;
    private final PlayerInterface       playerInterface;
    private final TournamentInterface   tournamentInterface;
    private final AwardInterface        awardInterface;
    private final MatchInterface        matchInterface;
    private final TableTennisEventInterface ttEventInterface;

    @Transactional
    public void onEventSaved(TableTennisEvent event) {
        if (event == null || event.getMatch() == null
                || event.getMatch().getTournament() == null || event.getPlayer() == null) return;
        Long tid = event.getMatch().getTournament().getId();
        ensureStats(event.getPlayer().getId(), tid, event.getMatch().getTournament());
        recalculatePlayerStats(event.getPlayer().getId(), tid);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
    public void onMatchEnd(Long matchId) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null) return;
        Long tid = match.getTournament().getId();

        Set<Long> pids = ttEventInterface.findByMatch_IdOrderByIdAsc(matchId)
                .stream().filter(e -> e.getPlayer() != null)
                .map(e -> e.getPlayer().getId()).collect(Collectors.toSet());

        for (Long pid : pids) {
            ensureStats(pid, tid, match.getTournament());
            recalculatePlayerStats(pid, tid);
        }
        calculatePOM(matchId, match);
        updateTournamentAwards(match.getTournament());
    }

    @Transactional
    public void recalculatePlayerStats(Long playerId, Long tournamentId) {
        Stats stats = statsInterface.findByPlayerIdAndTournamentId(playerId, tournamentId).orElse(null);
        if (stats == null) return;

        List<TableTennisEvent> events = ttEventInterface.findByPlayerIdAndTournamentId(playerId, tournamentId);
        int points = 0, attackShots = 0, faults = 0, outs = 0;

        for (TableTennisEvent ev : events) {
            if (ev.getPlayer() == null || !ev.getPlayer().getId().equals(playerId)) continue;
            switch (ev.getEventType().toUpperCase()) {
                case "POINT"        -> points++;
                case "SMASH"        -> { points++; attackShots++; }
                case "SERVICE_ACE"  -> { points++; attackShots++; }
                case "EDGE_BALL"    -> { points++; attackShots++; }
                case "NET_FAULT",
                     "SERVICE_FAULT"-> faults++;
                case "OUT"          -> outs++;
            }
        }

        stats.setGoals(points);
        stats.setAssists(attackShots);
        stats.setFouls(faults);
        stats.setYellowCards(outs);
        stats.setRedCards(0);

        int pom = awardInterface.countPomByPlayerIdAndSport(playerId, "table tennis");
        stats.setPlayerOfMatchCount(pom);
        stats.setPoints((points * 2) + (attackShots * 3) - (faults * 2) + (pom * 25));
        statsInterface.save(stats);
    }

    private void calculatePOM(Long matchId, Match match) {
        if (!awardInterface.findByMatchIdAndAwardType(matchId, "PLAYER_OF_MATCH").isEmpty()) return;
        Map<Long, Integer> scoreMap = new HashMap<>();
        Map<Long, Player>  playerMap = new HashMap<>();
        ttEventInterface.findByMatch_IdOrderByIdAsc(matchId).forEach(ev -> {
            if (ev.getPlayer() == null) return;
            Long pid = ev.getPlayer().getId();
            playerMap.put(pid, ev.getPlayer());
            int pts = switch (ev.getEventType().toUpperCase()) {
                case "SMASH","SERVICE_ACE","EDGE_BALL" -> 5;
                case "POINT"       -> 2;
                case "NET_FAULT",
                     "SERVICE_FAULT"-> -2;
                case "OUT"         -> -1;
                default            -> 0;
            };
            scoreMap.merge(pid, pts, Integer::sum);
        });
        if (scoreMap.isEmpty()) return;
        Long bestPid = scoreMap.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        if (bestPid == null) return;
        Award a = new Award();
        a.setMatch(match); a.setTournament(match.getTournament());
        a.setPlayer(playerMap.get(bestPid)); a.setAwardType("PLAYER_OF_MATCH");
        a.setPointsEarned(scoreMap.get(bestPid));
        a.setReason("Best TT performance: " + scoreMap.get(bestPid) + " pts");
        awardInterface.save(a);
        match.setManOfMatch(playerMap.get(bestPid));
        match.setStatus("COMPLETED");
        matchInterface.save(match);
    }

    @Transactional
    public void updateTournamentAwards(Tournament tournament) {
        awardInterface.findByTournamentId(tournament.getId()).stream()
                .filter(a -> a.getMatch() == null).forEach(awardInterface::delete);
        List<Stats> all = statsInterface.findAllByTournamentId(tournament.getId());
        if (all.isEmpty()) return;
        all.stream().filter(s -> s.getGoals() != null && s.getGoals() > 0)
                .max(Comparator.comparingInt(Stats::getGoals))
                .ifPresent(s -> save(null, tournament, s.getPlayer(), "TOP_SCORER", s.getGoals(), "Most points: " + s.getGoals()));
        all.stream().filter(s -> s.getAssists() != null && s.getAssists() > 0)
                .max(Comparator.comparingInt(Stats::getAssists))
                .ifPresent(s -> save(null, tournament, s.getPlayer(), "TOP_ATTACKER", s.getAssists(), "Most smashes/aces: " + s.getAssists()));
        List<Stats> top3 = all.stream()
                .filter(s -> s.getPoints() != null && s.getPoints() > 0)
                .sorted(Comparator.comparingInt(Stats::getPoints).reversed())
                .limit(3)
                .collect(Collectors.toList());

            int motRank = 1;
            for (Stats s : top3) {
                save(null, tournament, s.getPlayer(),
                        "MAN_OF_TOURNAMENT", s.getPoints(),
                        "Rank " + motRank++ + " - Tournament points: " + s.getPoints());
            }

    }

    private void ensureStats(Long pid, Long tid, Tournament t) {
        if (statsInterface.findByPlayerIdAndTournamentId(pid, tid).isPresent()) return;
        Player p = playerInterface.findActiveById(pid).orElse(null);
        if (p == null) return;
        Stats s = new Stats();
        s.setPlayer(p); s.setTournament(t); s.setSportType(t.getSport());
        s.setGoals(0); s.setAssists(0); s.setFouls(0); s.setYellowCards(0); s.setRedCards(0); s.setPoints(0);
        s.setRuns(0); s.setWickets(0); s.setBallsFaced(0); s.setBallsBowled(0); s.setRunsConceded(0);
        s.setFours(0); s.setSixes(0); s.setFifties(0); s.setHundreds(0); s.setHighest(0);
        s.setNotOut(0); s.setInningsPlayed(0); s.setMaidens(0); s.setThreeWicketHauls(0);
        s.setFiveWicketHauls(0); s.setDotBalls(0); s.setCatches(0); s.setRunouts(0); s.setStumpings(0);
        s.setPlayerOfMatchCount(0); s.setStrikeRate(0);
        s.setBattingAverage(0.0); s.setEconomy(0.0); s.setBowlingAverage(0.0); s.setBowlingStrikeRate(0.0);
        statsInterface.save(s);
    }

    private void save(Match m, Tournament t, Player p, String type, int pts, String reason) {
        Award a = new Award(); a.setMatch(m); a.setTournament(t); a.setPlayer(p);
        a.setAwardType(type); a.setPointsEarned(pts); a.setReason(reason);
        awardInterface.save(a);
    }
}


// ══ TableTennisPtsTableService.java ══════════════════════════════
// (In a separate file in practice)
// Win=2pts
