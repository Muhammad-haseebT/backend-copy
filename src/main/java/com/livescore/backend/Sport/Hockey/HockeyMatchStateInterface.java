package com.livescore.backend.Sport.Hockey;

import com.livescore.backend.Entity.Hockey.HockeyMatchState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HockeyMatchStateInterface extends JpaRepository<HockeyMatchState, Long> {
    Optional<HockeyMatchState> findByMatch_Id(Long matchId);
}
