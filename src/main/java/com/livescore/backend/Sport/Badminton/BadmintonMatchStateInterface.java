// ══════════════════════════════════════════════════════════════════
// BadmintonMatchStateInterface.java
// ══════════════════════════════════════════════════════════════════
package com.livescore.backend.Sport.Badminton;

import com.livescore.backend.Entity.Badminton.BadmintonMatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BadmintonMatchStateInterface extends JpaRepository<BadmintonMatchState, Long> {
    Optional<BadmintonMatchState> findByMatch_Id(Long matchId);
}
