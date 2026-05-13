// ══ TugOfWarStatsService.java ════════════════════════════════════
// Tug of War mein individual player stats nahi hote (team event hai)
// Stats: goals = rounds won (team level, not per player)
// POM: winning team ke captain ya random player ko dete hain
package com.livescore.backend.Sport.TugOfWar;

import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.TugOfWar.TugOfWarEvent;
import com.livescore.backend.Interface.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TugOfWarStatsService {

    private final AwardInterface        awardInterface;
    private final MatchInterface        matchInterface;
    private final TugOfWarEventInterface towEventInterface;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })

    public void onMatchEnd(Long matchId) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null) return;

        // POM: winning team ka first player
        if (!awardInterface.findByMatchIdAndAwardType(matchId, "PLAYER_OF_MATCH").isEmpty()) return;

        Team winner = match.getWinnerTeam();
        if (winner == null) return;

        // Get first player of winning team
        if (winner.getPlayers() == null || winner.getPlayers().isEmpty()) return;
        Player pom = winner.getPlayers().iterator().next();

        Award a = new Award();
        a.setMatch(match); a.setTournament(match.getTournament());
        a.setPlayer(pom); a.setAwardType("PLAYER_OF_MATCH");
        a.setPointsEarned(10);
        a.setReason("Team " + winner.getName() + " won the match");
        awardInterface.save(a);

        match.setManOfMatch(pom);
        match.setStatus("COMPLETED");
        matchInterface.save(match);
    }
}


// ══ TugOfWarPtsTableService.java ═════════════════════════════════
// (In separate file in practice)
