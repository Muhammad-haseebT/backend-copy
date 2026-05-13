package com.livescore.backend.Service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheEvictionService {

    private final CacheManager cacheManager;

    public CacheEvictionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictTournamentAwards(Long tournamentId) {
        if (tournamentId == null) return;
        if (cacheManager.getCache("tournamentAwards") != null) {
            cacheManager.getCache("tournamentAwards").evict(tournamentId);
        }
    }
}
