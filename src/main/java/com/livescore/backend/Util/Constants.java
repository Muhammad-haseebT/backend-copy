package com.livescore.backend.Util;

/**
 * Application-wide constants for cricket scoring, match statuses, and other configuration values.
 * Centralizes magic numbers and strings to improve code readability and maintainability.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // Match Status Constants
    public static final String STATUS_LIVE = "LIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_ABANDONED = "ABANDONED";
    public static final String STATUS_TIED = "TIED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";

    // Cricket Constants
    public static final int BALLS_PER_OVER = 6;
    public static final int DEFAULT_TEAM_SIZE = 11;
    public static final int DEFAULT_MAX_WICKETS = 10;
    public static final int FIRST_INNINGS = 1;
    public static final int SECOND_INNINGS = 2;

    // Event Types
    public static final String EVENT_RUN = "run";
    public static final String EVENT_BOUNDARY = "boundary";
    public static final String EVENT_BOUNDRY = "boundry"; // Keep typo for backward compatibility
    public static final String EVENT_WIDE = "wide";
    public static final String EVENT_NO_BALL = "noball";
    public static final String EVENT_NB = "nb";
    public static final String EVENT_BYE = "bye";
    public static final String EVENT_LEG_BYE = "legbye";
    public static final String EVENT_WICKET = "wicket";

    // Extra Types
    public static final String EXTRA_WIDE = "WIDE";
    public static final String EXTRA_NO_BALL = "NO_BALL";
    public static final String EXTRA_BYE = "BYE";
    public static final String EXTRA_LEG_BYE = "LEGBYE";

    // Dismissal Types
    public static final String DISMISSAL_CAUGHT = "caught";
    public static final String DISMISSAL_RUNOUT = "runout";
    public static final String DISMISSAL_STUMPED = "stumped";
    public static final String DISMISSAL_BOWLED = "bowled";
    public static final String DISMISSAL_LBW = "lbw";

    // Boundary Values
    public static final int BOUNDARY_FOUR = 4;
    public static final int BOUNDARY_SIX = 6;

    // Decision Types
    public static final String DECISION_BAT = "bat";
    public static final String DECISION_BOWL = "bowl";

    // Sports Names
    public static final String SPORT_CRICKET = "CRICKET";
    public static final String SPORT_FUTSAL = "FUTSAL";
    public static final String SPORT_FOOTBALL = "FOOTBALL";
    public static final String SPORT_VOLLEYBALL = "VOLLEYBALL";
    public static final String SPORT_BADMINTON = "BADMINTON";
    public static final String SPORT_TABLE_TENNIS = "TABLETENNIS";

    // Award Calculation Weights
    public static final double WEIGHT_RUN = 1.0;
    public static final double WEIGHT_WICKET = 25.0;
    public static final double WEIGHT_SIX = 2.0;
    public static final double WEIGHT_FOUR = 1.0;
    public static final double WEIGHT_POM = 15.0;
    public static final double WEIGHT_STRIKE_RATE_DIVISOR = 20.0;
    public static final double WEIGHT_ECONOMY_PENALTY = 5.0;
    public static final double ECONOMY_THRESHOLD = 8.0;
    public static final int MIN_BALLS_FOR_STRIKE_RATE = 10;
    public static final double MAX_STRIKE_RATE_CAP = 200.0;

    // Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CAPTAIN = "CAPTAIN";

    // Status Messages
    public static final String STATUS_END = "END";
    public static final String STATUS_END_FIRST = "END_FIRST";
    public static final String STATUS_END_MATCH = "END_MATCH";
    public static final String STATUS_ERROR = "ERROR";

    // Error Messages
    public static final String ERROR_MATCH_NOT_FOUND = "Match not found";
    public static final String ERROR_INNINGS_NOT_FOUND = "Innings not found";
    public static final String ERROR_MATCH_ALREADY_ENDED = "Match already ended";
    public static final String ERROR_DUPLICATE_BALL = "Duplicate ball";
    public static final String ERROR_INVALID_EVENT_TYPE = "Invalid event type";

    // Top Lists Limits
    public static final int TOP_BATSMEN_LIMIT = 5;
    public static final int TOP_BOWLERS_LIMIT = 5;
}
