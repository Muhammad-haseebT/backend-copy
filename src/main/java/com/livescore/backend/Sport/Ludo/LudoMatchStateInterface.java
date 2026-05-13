// ══ LudoMatchStateInterface.java ════════════════════════════════
package com.livescore.backend.Sport.Ludo;

import com.livescore.backend.Entity.Ludo.LudoMatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LudoMatchStateInterface extends JpaRepository<LudoMatchState, Long> {
    Optional<LudoMatchState> findByMatch_Id(Long matchId);
}
