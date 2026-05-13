package com.livescore.backend.Service;

import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Util.CricketRules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Helper class for aggregating player statistics from cricket ball data.
 * Reduces code duplication in AwardService by providing reusable aggregation logic.
 */
public class AggregationHelper {

    /**
     * Aggregate class for storing player statistics during calculation.
     */
    public static class PlayerAggregate {
        public long playerId;
        public int runs = 0;
        public int ballsFaced = 0;
        public int fours = 0;
        public int sixes = 0;
        public int wickets = 0;
        public int runsConceded = 0;
        public int ballsBowled = 0;
        public Map<Long, Integer> inningsRuns = new HashMap<>();
        public int pomCount = 0;
    }

    /**
     * Player metrics calculated from aggregated data.
     */
    public static class PlayerMetrics {
        public Long playerId;
        public int runs;
        public int ballsFaced;
        public double strikeRate;
        public int highestInnings;
        public int fours;
        public int sixes;
        public int wickets;
        public int runsConceded;
        public int ballsBowled;
        public double economy;
        public double bowlingAverage;
        public int pomCount;
    }

    /**
     * Creates a function that returns or creates a PlayerAggregate for a given player.
     *
     * @param aggregateMap Map to store aggregates
     * @return Function that gets or creates aggregate
     */
    public static Function<Player, PlayerAggregate> createAggregateFunction(Map<Long, PlayerAggregate> aggregateMap) {
        return (player) -> {
            if (player == null) return null;
            return aggregateMap.computeIfAbsent(player.getId(), id -> {
                PlayerAggregate a = new PlayerAggregate();
                a.playerId = id;
                return a;
            });
        };
    }

    /**
     * Processes batting statistics from cricket balls.
     *
     * @param balls          List of cricket balls
     * @param getAggFunction Function to get player aggregate
     */
    public static void processBattingStats(List<CricketBall> balls, Function<Player, PlayerAggregate> getAggFunction) {
        for (CricketBall b : balls) {
            if (b.getBatsman() == null) continue;

            Long inningsId = (b.getInnings() == null) ? null : b.getInnings().getId();
            PlayerAggregate a = getAggFunction.apply(b.getBatsman());
            if (a == null) continue;

            int runs = (b.getRuns() == null ? 0 : b.getRuns());
            a.runs += runs;

            if (CricketRules.isBallFaced(b)) {
                a.ballsFaced += 1;
            }

            if (Boolean.TRUE.equals(b.getIsFour())) {
                a.fours += 1;
            }

            if (Boolean.TRUE.equals(b.getIsSix())) {
                a.sixes += 1;
            }

            if (inningsId != null) {
                a.inningsRuns.put(inningsId, a.inningsRuns.getOrDefault(inningsId, 0) + runs);
            }
        }
    }

    /**
     * Processes bowling statistics from cricket balls.
     *
     * @param balls          List of cricket balls
     * @param getAggFunction Function to get player aggregate
     */
    public static void processBowlingStats(List<CricketBall> balls, Function<Player, PlayerAggregate> getAggFunction) {
        for (CricketBall b : balls) {
            if (b.getBowler() == null) continue;

            PlayerAggregate a = getAggFunction.apply(b.getBowler());
            if (a == null) continue;

            a.runsConceded += CricketRules.runsConcededThisBall(b);

            if (Boolean.TRUE.equals(b.getLegalDelivery())) {
                a.ballsBowled += 1;
            }

            String dismissal = b.getDismissalType();
            if (dismissal != null && CricketRules.isBowlerCreditedWicket(dismissal)) {
                a.wickets += 1;
            }
        }
    }

    /**
     * Converts a PlayerAggregate to PlayerMetrics with calculated values.
     *
     * @param agg Player aggregate data
     * @return Player metrics with calculated strike rate, economy, etc.
     */
    public static PlayerMetrics calculateMetrics(PlayerAggregate agg) {
        PlayerMetrics pm = new PlayerMetrics();
        pm.playerId = agg.playerId;
        pm.runs = agg.runs;
        pm.ballsFaced = agg.ballsFaced;
        pm.fours = agg.fours;
        pm.sixes = agg.sixes;
        pm.wickets = agg.wickets;
        pm.runsConceded = agg.runsConceded;
        pm.ballsBowled = agg.ballsBowled;
        pm.highestInnings = agg.inningsRuns.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        pm.pomCount = agg.pomCount;

        // Calculate strike rate
        if (pm.ballsFaced > 0) {
            pm.strikeRate = ((double) pm.runs * 100.0) / (double) pm.ballsFaced;
        } else {
            pm.strikeRate = 0.0;
        }

        // Calculate economy rate
        if (pm.ballsBowled > 0) {
            double overs = pm.ballsBowled / 6.0;
            pm.economy = overs > 0 ? ((double) pm.runsConceded) / overs : Double.POSITIVE_INFINITY;
        } else {
            pm.economy = Double.POSITIVE_INFINITY;
        }

        // Calculate bowling average
        if (pm.wickets > 0) {
            pm.bowlingAverage = ((double) pm.runsConceded) / pm.wickets;
        } else {
            pm.bowlingAverage = Double.POSITIVE_INFINITY;
        }

        return pm;
    }
}
