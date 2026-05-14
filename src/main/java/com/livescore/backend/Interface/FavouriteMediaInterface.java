package com.livescore.backend.Interface;

import com.livescore.backend.Entity.FavouriteMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavouriteMediaInterface extends JpaRepository<FavouriteMedia, Long> {
    
    boolean existsByAccountIdAndMediaId(Long accountId, Long mediaId);
    
    Optional<FavouriteMedia> findByAccountIdAndMediaId(Long accountId, Long mediaId);
    
    List<FavouriteMedia> findByAccountId(Long accountId);
    
    List<FavouriteMedia> findByMatchIdAndAccountId(Long matchId, Long accountId);
    
}
