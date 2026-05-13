package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class PtsTableDTO {

    private Long   id;
    private Long   teamId;
    private String teamName;
    private Long   tournamentId;

    // ── Common ────────────────────────────────────────────────────
    private int played;
    private int wins;
    private int losses;
    private int points;

    // ── Cricket ──────────────────────────────────────────────────
    private double nrr;

    // ── Futsal ───────────────────────────────────────────────────
    private int draws;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference; // calculated: goalsFor - goalsAgainst
    private String sport; // "cricket" / "futsal" — frontend uses this
}