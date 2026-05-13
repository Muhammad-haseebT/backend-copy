package com.livescore.backend.Interface;

import com.livescore.backend.Entity.FavouritePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface FavouritePlayerInterface extends JpaRepository<FavouritePlayer, Long> {
    Optional<FavouritePlayer> findByMatchId(Long matchId);
}
