package com.livescore.backend.DTO;

import lombok.Data;
@Data
public class PlayerStatDTO {
    public Long playerId;
    public String playerName;

    // batting
    public Integer runs = 0;
    public Integer ballsFaced = 0;
    public Integer fours = 0;
    public Integer sixes = 0;

    // bowling
    public Integer wickets = 0;
    public Integer runsConceded = 0;
    public Integer ballsBowled = 0;
    public Double economy = null;        // runs per over (null if never bowled)
    public Double bowlingAverage = null; // runsConceded / wickets (null if wickets == 0)

    // meta
    public Integer pomCount = 0;     // player-of-match count
    public Double compositeScore = 0.0;
    private double strikeRate=0.0;
}
