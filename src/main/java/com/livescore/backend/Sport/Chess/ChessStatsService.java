package com.livescore.backend.Sport.Chess;

import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Chess.ChessEvent;
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
public class ChessStatsService {

    private final StatsInterface       statsInterface;
    private final PlayerInterface      playerInterface;
    private final AwardInterface       awardInterface;
    private final MatchInterface       matchInterface;
    private final ChessEventInterface  chessEventInterface;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
    public void onMatchEnd(Long matchId) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null) return;
        Long tid = match.getTournament().getId();

        // Ensure stats exist for both teams' players
        if (match.getTeam1() != null && match.getTeam1().getPlayers() != null)
            match.getTeam1().getPlayers().forEach(p -> ensureStats(p.getId(), tid, match.getTournament()));
        if (match.getTeam2() != null && match.getTeam2().getPlayers() != null)
            match.getTeam2().getPlayers().forEach(p -> ensureStats(p.getId(), tid, match.getTournament()));

        // Increment wins (goals) for winning team players
        if (match.getWinnerTeam() != null && match.getWinnerTeam().getPlayers() != null) {
            match.getWinnerTeam().getPlayers().forEach(p -> {
                statsInterface.findByPlayerIdAndTournamentId(p.getId(), tid).ifPresent(s -> {
                    s.setGoals(safe(s.getGoals()) + 1);
                    int pom = awardInterface.countPomByPlayerIdAndSport(p.getId(), "chess");
                    s.setPlayerOfMatchCount(pom);
                    s.setPoints((safe(s.getGoals()) * 10) + (pom * 25));
                    statsInterface.save(s);
                });
            });
        }
        match.setStatus("COMPLETED");
        matchInterface.save(match);

        calculatePOM(matchId, match);
        updateTournamentAwards(match.getTournament());
    }

    private void calculatePOM(Long matchId, Match match) {
        if (!awardInterface.findByMatchIdAndAwardType(matchId, "PLAYER_OF_MATCH").isEmpty()) return;
        if (match.getWinnerTeam() == null) return;

        Team winner = match.getWinnerTeam();
        if (winner.getPlayers() == null || winner.getPlayers().isEmpty()) return;

        // POM = first player of winning team (chess 1v1 typically)
        Player pom = winner.getPlayers().iterator().next();

        Award a = new Award();
        a.setMatch(match); a.setTournament(match.getTournament());
        a.setPlayer(pom); a.setAwardType("PLAYER_OF_MATCH");
        a.setPointsEarned(10); a.setReason("Winner of chess match");
        awardInterface.save(a);
        match.setManOfMatch(pom);
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
                .ifPresent(s -> saveAward(null, tournament, s.getPlayer(),
                        "TOP_SCORER", s.getGoals(), "Most wins: " + s.getGoals()));
        all.stream().filter(s -> s.getPoints() != null && s.getPoints() > 0)
                .max(Comparator.comparingInt(Stats::getPoints))
                .ifPresent(s -> saveAward(null, tournament, s.getPlayer(),
                        "MAN_OF_TOURNAMENT", s.getPoints(), "Highest pts: " + s.getPoints()));
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

    private void saveAward(Match m, Tournament t, Player p, String type, int pts, String reason) {
        Award a = new Award(); a.setMatch(m); a.setTournament(t); a.setPlayer(p);
        a.setAwardType(type); a.setPointsEarned(pts); a.setReason(reason);
        awardInterface.save(a);
    }

    private int safe(Integer v) { return v != null ? v : 0; }
}