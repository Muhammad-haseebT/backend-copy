package com.livescore.backend.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CricketBallsScoringDTO {

    Long id;
    String event;
    String eventType;
    int mediaCount;
}
