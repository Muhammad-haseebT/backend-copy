package com.livescore.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SeasonSportsRequestDTO {
    private Long seasonId;
    private List<Long> sportsIds;

}
