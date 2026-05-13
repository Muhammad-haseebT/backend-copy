package com.livescore.backend.Sport.Futsal;

import com.livescore.backend.Entity.Futsal.FutsalMatchState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Interface/Futsal/FutsalMatchStateInterface.java
public interface FutsalMatchStateInterface extends JpaRepository<FutsalMatchState, Long> {
    Optional<FutsalMatchState> findByMatch_Id(Long matchId);
}
