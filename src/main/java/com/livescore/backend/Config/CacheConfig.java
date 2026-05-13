package com.livescore.backend.Config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching  // ✅ Add this annotation
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // ✅ Add "matchState" alongside "tournamentAwards"
        return new ConcurrentMapCacheManager("tournamentAwards", "matchStates","inningsState","tournamentStats","matches","matchById","matchStates","playerStats","teamStats","seasons","allSeasons","SeasonNames","seasonById","tournamentOverview","tournamentNames","tournamentById","tournaments","teamByTournamentId","teams","teamById","teamByTournamentIdAndAccountId","teamByPlayers","futsalStates","futsalStateByMatchId","volleyballStates","volleyballStateByMatchId","badmintonStates","badmintonStateByMatchId","ttStates","ttStateByMatchId","towStates","towStateByMatchId","ludoStates","ludoStateByMatchId","chessStates","chessStateByMatchId","vbStates","vbStateByMatchId");
    }
}
