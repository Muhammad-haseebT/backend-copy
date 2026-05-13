package com.livescore.backend.Interface.multisportgeneric;

import com.fasterxml.jackson.databind.JsonNode;
import com.livescore.backend.DTO.ScoringDTOs.ScoreDTO;
// multisportgeneric/ScoringServiceInterface.java
public interface ScoringServiceInterface {

    /**
     * Current match state fetch karo (on WebSocket connect)
     */
    Object getCurrentMatchState(Long matchId);

    /**
     * Raw JsonNode pass hoga — har service apna DTO khud banayegi
     */
    Object scoring(JsonNode rawPayload);

    /**
     * Undo last event.
     * Cricket: inningsId use karta hai
     * Futsal: inningsId ignore kar sakta hai (null pass ho sakta hai)
     */
    Object undoLastBall(Long matchId, Long inningsId);
}