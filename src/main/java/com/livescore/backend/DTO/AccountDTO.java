package com.livescore.backend.DTO;

import lombok.Data;

@Data
public class AccountDTO {
    private Long id;
    private Long playerId;
    private String username;
    private String password;
    private String name;
    private String role;
}
