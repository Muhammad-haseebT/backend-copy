package com.livescore.backend.Service;

import com.livescore.backend.DTO.TournamentAwardsDTO;
import com.livescore.backend.DTO.TournamentAwardsDTO.AwardDTO;
import com.livescore.backend.DTO.TournamentAwardsDTO.PlayerStatsRow;
import com.livescore.backend.Entity.Award;
import com.livescore.backend.Entity.Stats;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.AwardInterface;
import com.livescore.backend.Interface.StatsInterface;
import com.livescore.backend.Interface.TournamentInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AwardService {

    private final AwardInterface      awardInterface;
    private final StatsInterface      statsInterface;
    private final TournamentInterface tournamentInterface;
    private final StatsService        statsService;

    public TournamentAwardsDTO getTournamentStats(Long tournamentId) {
        Tournament tournament = tournamentInterface.findById(tournamentId).orElse(null);
        if (tournament == null) return null;

        TournamentAwardsDTO dto = new TournamentAwardsDTO();
        dto.setTournamentId(tournamentId);
        dto.setTournamentName(tournament.getName());

        String sport = tournament.getSport() != null
                ? tournament.getSport().getName().toLowerCase() : "cricket";
        dto.setSport(sport);

        List<Award> awards = awardInterface.findByTournamentId(tournamentId);

        // POM list
        dto.setAllAwards(awards.stream()
                .filter(a -> "PLAYER_OF_MATCH".equals(a.getAwardType()))
                .map(this::toAwardDTO).collect(Collectors.toList()));

        // ── NEW: top 3 MAN_OF_TOURNAMENT ─────────────────────────────
        List<AwardDTO> motList = awards.stream()
                .filter(a -> "MAN_OF_TOURNAMENT".equals(a.getAwardType()))
                .limit(3)
                .map(this::toAwardDTO)
                .collect(Collectors.toList());
        dto.setManOfTournament(motList);
        // ─────────────────────────────────────────────────────────────

        List<Stats> allStats = statsInterface.findAllByTournamentId(tournamentId);

        switch (sport) {
            case "futsal"                     -> buildFutsalStats(dto, awards, allStats);
            case "volleyball"                 -> buildVolleyballStats(dto, awards, allStats);
            case "badminton"                  -> buildBadmintonStats(dto, awards, allStats);
            case "table tennis", "tabletennis" -> buildTableTennisStats(dto, awards, allStats); // fixes typo warning too
            case "ludo"                       -> buildLudoStats(dto, awards, allStats);
            case "chess"                      -> buildChessStats(dto, awards, allStats);
            default                           -> buildCricketStats(dto, awards, allStats);
        }

        // ── NEW: favourite player — AFTER allStats is declared ────────
        awards.stream()
                .filter(a -> "FAVOURITE_PLAYER".equals(a.getAwardType()))
                .findFirst()
                .ifPresent(a -> dto.setFavouritePlayer(toAwardDTO(a)));
        // ─────────────────────────────────────────────────────────────

        return dto;
    }
    private void buildCricketStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a->"BEST_BATSMAN".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setBestBatsman(toAwardDTO(a)));
        awards.stream().filter(a->"BEST_BOWLER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setBestBowler(toAwardDTO(a)));
        awards.stream().filter(a->"BEST_FIELDER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setBestFielder(toAwardDTO(a)));
        dto.setTopRunScorers(topN(s, Comparator.comparingInt(Stats::getRuns).reversed()));
        dto.setTopBowlers(topN(s, Comparator.comparingInt(Stats::getWickets).reversed().thenComparingDouble(x->x.getEconomy()!=null?x.getEconomy():999)));
    }

    private void buildFutsalStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a->"TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a->"TOP_ASSIST".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(topN(s, Comparator.comparingInt(Stats::getGoals).reversed()));
        dto.setTopAssisters(topN(s, Comparator.comparingInt(Stats::getAssists).reversed()));
    }

    private void buildVolleyballStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a->"TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a->"BEST_SERVER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(topN(s, Comparator.comparingInt(Stats::getGoals).reversed()));
        dto.setTopAssisters(topN(s, Comparator.comparingInt(Stats::getAssists).reversed()));
    }

    private void buildBadmintonStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a->"TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a->"TOP_ATTACKER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(topN(s, Comparator.comparingInt(Stats::getGoals).reversed()));
        dto.setTopAssisters(topN(s, Comparator.comparingInt(Stats::getAssists).reversed()));
    }

    private void buildTableTennisStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a->"TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a->"TOP_ATTACKER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(topN(s, Comparator.comparingInt(Stats::getGoals).reversed()));
        dto.setTopAssisters(topN(s, Comparator.comparingInt(Stats::getAssists).reversed()));
    }

    private void buildLudoStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a->"TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a->"TOP_ATTACKER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(topN(s, Comparator.comparingInt(Stats::getGoals).reversed()));
        dto.setTopAssisters(topN(s, Comparator.comparingInt(Stats::getAssists).reversed()));
    }

    // ── Chess: goals=wins, assists=checks ────────────────────────
    private void buildChessStats(TournamentAwardsDTO dto, List<Award> awards, List<Stats> s) {
        awards.stream().filter(a->"TOP_SCORER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopScorer(toAwardDTO(a)));
        awards.stream().filter(a->"TOP_ATTACKER".equals(a.getAwardType())).findFirst().ifPresent(a->dto.setTopAssist(toAwardDTO(a)));
        dto.setTopGoalScorers(topN(s, Comparator.comparingInt(Stats::getGoals).reversed()));
        dto.setTopAssisters(topN(s, Comparator.comparingInt(Stats::getAssists).reversed()));
    }

    public TournamentAwardsDTO recalculateAndGetStats(Long tournamentId) {
        Tournament t = tournamentInterface.findById(tournamentId).orElse(null);
        if (t == null) return null;
        statsService.checkAndHandleTournamentEnd(t);
        return getTournamentStats(tournamentId);
    }

    private List<PlayerStatsRow> topN(List<Stats> s, Comparator<Stats> cmp) {
        return s.stream().sorted(cmp).limit(5).map(this::toPlayerStatsRow).collect(Collectors.toList());
    }

    private AwardDTO toAwardDTO(Award award) {
        AwardDTO dto = new AwardDTO();
        dto.setPlayerId(award.getPlayer().getId()); dto.setPlayerName(award.getPlayer().getName());
        dto.setAwardType(award.getAwardType());
        dto.setPoints(award.getPointsEarned() != null ? award.getPointsEarned() : 0);
        dto.setReason(award.getReason()); return dto;
    }

    private PlayerStatsRow toPlayerStatsRow(Stats s) {
        PlayerStatsRow r = new PlayerStatsRow();
        r.setPlayerId(s.getPlayer().getId()); r.setPlayerName(s.getPlayer().getName());
        r.setRuns(safe(s.getRuns())); r.setWickets(safe(s.getWickets()));
        r.setBallsFaced(safe(s.getBallsFaced())); r.setBallsBowled(safe(s.getBallsBowled()));
        r.setFours(safe(s.getFours())); r.setSixes(safe(s.getSixes()));
        r.setHighest(safe(s.getHighest())); r.setStrikeRate(s.getStrikeRate());
        r.setCatches(safe(s.getCatches())); r.setRunouts(safe(s.getRunouts()));
        r.setStumpings(safe(s.getStumpings())); r.setFifties(safe(s.getFifties()));
        r.setHundreds(safe(s.getHundreds())); r.setMaidens(safe(s.getMaidens()));
        r.setDotBalls(safe(s.getDotBalls())); r.setRunsConceded(safe(s.getRunsConceded()));
        r.setEconomy(s.getEconomy());
        r.setGoals(safe(s.getGoals())); r.setAssists(safe(s.getAssists()));
        r.setFutsalFouls(safe(s.getFouls())); r.setYellowCards(safe(s.getYellowCards())); r.setRedCards(safe(s.getRedCards()));
        r.setPlayerOfMatchCount(safe(s.getPlayerOfMatchCount())); r.setTotalPoints(safe(s.getPoints()));
        return r;
    }

    private int safe(Integer v) { return v != null ? v : 0; }
}