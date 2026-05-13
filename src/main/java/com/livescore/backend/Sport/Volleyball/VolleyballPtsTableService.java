package com.livescore.backend.Sport.Volleyball;

import com.livescore.backend.DTO.PtsTableDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Volleyball points table:
 *   3-0 or 3-1 win  → winner 3 pts, loser 0 pts
 *   3-2 win         → winner 3 pts, loser 1 pt   (competitive set bonus)
 *
 * Sorted by: Points → Set Ratio → Points Ratio
 */
@Service
@RequiredArgsConstructor
public class VolleyballPtsTableService {

    private final PtsTableInterface ptsTableInterface;
    private final MatchInterface matchInterface;

    @Transactional
    public void updateAfterMatch(Long matchId, int team1Sets, int team2Sets) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null) return;

        Long tournamentId = match.getTournament().getId();
        Team team1 = match.getTeam1();
        Team team2 = match.getTeam2();

        PtsTable pts1 = getOrCreate(tournamentId, team1, match.getTournament());
        PtsTable pts2 = getOrCreate(tournamentId, team2, match.getTournament());

        pts1.setPlayed(safe(pts1.getPlayed()) + 1);
        pts2.setPlayed(safe(pts2.getPlayed()) + 1);

        boolean team1Won = team1Sets > team2Sets;
        int totalSets = team1Sets + team2Sets;

        if (team1Won) {
            pts1.setWins(safe(pts1.getWins()) + 1);
            pts2.setLosses(safe(pts2.getLosses()) + 1);
            pts1.setPoints(safe(pts1.getPoints()) + 3);
            // 3-2: loser gets 1 bonus pt
            if (totalSets == 5) pts2.setPoints(safe(pts2.getPoints()) + 1);
        } else {
            pts2.setWins(safe(pts2.getWins()) + 1);
            pts1.setLosses(safe(pts1.getLosses()) + 1);
            pts2.setPoints(safe(pts2.getPoints()) + 3);
            if (totalSets == 5) pts1.setPoints(safe(pts1.getPoints()) + 1);
        }

        // Track sets for/against (stored in goalsFor/Against)
        pts1.setGoalsFor(safe(pts1.getGoalsFor()) + team1Sets);
        pts1.setGoalsAgainst(safe(pts1.getGoalsAgainst()) + team2Sets);
        pts2.setGoalsFor(safe(pts2.getGoalsFor()) + team2Sets);
        pts2.setGoalsAgainst(safe(pts2.getGoalsAgainst()) + team1Sets);

        ptsTableInterface.save(pts1);
        ptsTableInterface.save(pts2);
    }

    public ResponseEntity<?> getTable(Long tournamentId) {
        List<PtsTable> tables = ptsTableInterface.findByTournamentId(tournamentId);

        List<PtsTableDTO> dtos = tables.stream().map(pt -> {
            PtsTableDTO dto = new PtsTableDTO();
            dto.setId(pt.getId());
            dto.setTeamId(pt.getTeam().getId());
            dto.setTeamName(pt.getTeam().getName());
            dto.setTournamentId(tournamentId);
            dto.setPlayed(safe(pt.getPlayed()));
            dto.setWins(safe(pt.getWins()));
            dto.setLosses(safe(pt.getLosses()));
            dto.setPoints(safe(pt.getPoints()));
            // Sets for/against stored in goalsFor/goalsAgainst
            dto.setGoalsFor(safe(pt.getGoalsFor()));         // sets won
            dto.setGoalsAgainst(safe(pt.getGoalsAgainst())); // sets lost
            dto.setGoalDifference(safe(pt.getGoalsFor()) - safe(pt.getGoalsAgainst())); // set diff
            return dto;
        }).sorted(Comparator
                .comparingInt(PtsTableDTO::getPoints).reversed()
                .thenComparingInt(PtsTableDTO::getGoalDifference).reversed()
                .thenComparingInt(PtsTableDTO::getGoalsFor).reversed())
        .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    private PtsTable getOrCreate(Long tournamentId, Team team, Tournament tournament) {
        PtsTable existing = ptsTableInterface.findByTournamentIdAndTeamId(tournamentId, team.getId());
        if (existing != null) return existing;
        PtsTable pt = new PtsTable();
        pt.setTeam(team); pt.setTournament(tournament);
        pt.setPlayed(0); pt.setWins(0); pt.setLosses(0); pt.setDraws(0);
        pt.setPoints(0); pt.setGoalsFor(0); pt.setGoalsAgainst(0);
        return ptsTableInterface.save(pt);
    }

    private int safe(Integer v) { return v == null ? 0 : v; }
}
