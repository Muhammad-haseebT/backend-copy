package com.livescore.backend.Sport.Volleyball;

import com.livescore.backend.Entity.Volleyball.VolleyballMatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VolleyballMatchStateInterface extends JpaRepository<VolleyballMatchState, Long> {
    Optional<VolleyballMatchState> findByMatch_Id(Long matchId);
}
