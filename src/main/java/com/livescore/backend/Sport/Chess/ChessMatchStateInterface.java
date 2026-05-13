// ══ ChessMatchStateInterface.java ════════════════════════════════
package com.livescore.backend.Sport.Chess;

import com.livescore.backend.Entity.Chess.ChessMatchState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChessMatchStateInterface extends JpaRepository<ChessMatchState, Long> {
    Optional<ChessMatchState> findByMatch_Id(Long matchId);
}
