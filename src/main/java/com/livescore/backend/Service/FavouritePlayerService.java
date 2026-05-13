package com.livescore.backend.Service;

import com.livescore.backend.Entity.FavouritePlayer;
import com.livescore.backend.Interface.FavouritePlayerInterface;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.PlayerInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavouritePlayerService {

    private final FavouritePlayerInterface fpRepo;
    private final MatchInterface matchRepo;
    private final PlayerInterface playerInterface;

    public List<Map<String, Object>> getTopVotedPlayersForTournament(Long tournamentId) {

        List<com.livescore.backend.Entity.Match> matches = matchRepo.findByTournamentId(tournamentId);

        // playerId -> {name, votes} aggregate map
        Map<Long, Long> totalVotes = new HashMap<>();
        Map<Long, String> playerNames = new HashMap<>();

        for (var match : matches) {
            fpRepo.findByMatchId(match.getId()).ifPresent(fp -> {
                fp.getPlayerVoteCounts().forEach((playerId, count) -> {
                    totalVotes.merge(playerId, count.longValue(), Long::sum);
                });
            });
        }

        // player names ke liye playerInterface use karo
        return totalVotes.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(3)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("playerId", e.getKey());
                    m.put("votes", e.getValue());
                    playerInterface.findById(e.getKey()).ifPresent(p -> m.put("playerName", p.getName()));
                    return m;
                })
                .collect(Collectors.toList());
    }
    public String submitVote(Long matchId, Long accountId, Long playerId, String feedback) {
        FavouritePlayer fp = fpRepo.findByMatchId(matchId)
                .orElseGet(() -> {
                    FavouritePlayer newFp = new FavouritePlayer();
                    newFp.setMatch(matchRepo.findById(matchId)
                            .orElseThrow(() -> new RuntimeException("Match not found")));
                    return newFp;
                });

        if (fp.getVotedAccountIds().contains(accountId)) {
            throw new RuntimeException("Aap pehle hi vote kar chuke hain!");
        }

        fp.getVotedAccountIds().add(accountId);
        fp.getPlayerVoteCounts().merge(playerId, 1, Integer::sum);

        // feedback optional hai — null ya empty ho to skip
        if (feedback != null && !feedback.isBlank()) {
            fp.getFeedbacks().put(accountId, feedback);
        }

        fpRepo.save(fp);
        return "Vote submit ho gaya!";
    }
    public FavouritePlayer getResults(Long matchId) {
        return fpRepo.findByMatchId(matchId)
                .orElseThrow(() -> new RuntimeException("Koi votes nahi abhi tak!"));
    }
    public boolean checkVote(Long matchId, Long accountId) {
        FavouritePlayer fp = fpRepo.findByMatchId(matchId).orElse(null);
        if (fp == null) {
            return false;
        }else return fp.getVotedAccountIds().contains(accountId);

    }
}