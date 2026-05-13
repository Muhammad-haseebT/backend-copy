package com.livescore.backend.Util;

import com.livescore.backend.Entity.CricketBall;

public final class CricketRules {

    private CricketRules() {
    }

    public static String normalizeExtraType(String extraType) {
        return extraType == null ? "" : extraType.toLowerCase();
    }

    public static boolean isWide(String extraType) {
        return normalizeExtraType(extraType).contains("wide");
    }

    public static boolean isNoBall(String extraType) {
        // keep matching lenient because existing data may contain multiple spellings
        return normalizeExtraType(extraType).contains("no");
    }

    public static boolean isByeOrLegBye(String extraType) {
        return normalizeExtraType(extraType).contains("bye");
    }

    /**
     * Wide does not count as a ball faced; no-ball counts as faced even when illegal.
     */
    public static boolean isBallFaced(CricketBall ball) {
        if (ball == null) return false;
        String extraType = ball.getExtraType();
        boolean wide = isWide(extraType);
        boolean noBall = isNoBall(extraType);
        return !wide && (Boolean.TRUE.equals(ball.getLegalDelivery()) || noBall);
    }

    /**
     * Byes/leg-byes are not charged to bowler. Wide/no-ball extras are charged.
     */
    public static int runsConcededThisBall(CricketBall ball) {
        if (ball == null) return 0;
        int baseRuns = (ball.getRuns() == null ? 0 : ball.getRuns());
        int extraRuns = (ball.getExtra() == null ? 0 : ball.getExtra());
        return isByeOrLegBye(ball.getExtraType()) ? baseRuns : (baseRuns + extraRuns);
    }

    /**
     * Wickets credited to bowler (run-out/retired are excluded).
     */
    public static boolean isBowlerCreditedWicket(String dismissalType) {
        if (dismissalType == null) return false;
        String d = dismissalType.toLowerCase();
        return d.contains("bowled")
                || d.contains("lbw")
                || d.contains("stumped")
                || d.contains("caught")
                || d.contains("hit wicket")
                || d.contains("hitwicket")
                || d.contains("hit-wicket");
    }
}
