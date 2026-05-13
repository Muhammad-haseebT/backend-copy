package com.livescore.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data

public class ShowRequestDTO {

    private Long requestId;
    private String teamName;
    private String status;
    private Long teamId;
    private Long tournamentId;
}
