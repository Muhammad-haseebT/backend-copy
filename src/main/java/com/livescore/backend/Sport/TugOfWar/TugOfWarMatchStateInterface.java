// ══ TugOfWarMatchStateInterface.java ════════════════════════════
package com.livescore.backend.Sport.TugOfWar;

import com.livescore.backend.Entity.TugOfWar.TugOfWarMatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TugOfWarMatchStateInterface extends JpaRepository<TugOfWarMatchState, Long> {
    Optional<TugOfWarMatchState> findByMatch_Id(Long matchId);
}
