package com.livescore.backend.Service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SportTournamentCount {
    private String name;
    private Long tournamentCount;
    private Long sportId;

}
