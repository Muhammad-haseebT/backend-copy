package com.livescore.backend.DTO;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlayerDTO {
    private Long id;
    private String name;
    private String playerRole;
    private String username;

    private List<ShowRequestDTO> playerRequests = new ArrayList<>();
}
