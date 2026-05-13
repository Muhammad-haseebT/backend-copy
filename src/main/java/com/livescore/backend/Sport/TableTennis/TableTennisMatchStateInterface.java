// ══ TableTennisMatchStateInterface.java ══════════════════════════
package com.livescore.backend.Sport.TableTennis;

import com.livescore.backend.Entity.TableTennis.TableTennisMatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TableTennisMatchStateInterface extends JpaRepository<TableTennisMatchState, Long> {
    Optional<TableTennisMatchState> findByMatch_Id(Long matchId);
}
