package com.livescore.backend.Sport.Futsal;

import com.livescore.backend.DTO.PtsTableDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FutsalPtsTableService {

    private final PtsTableInterface ptsTableInterface;
    private final MatchInterface matchInterface;

    @Transactional
    public ResponseEntity<?> updateAfterMatch(Long matchId) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null) return ResponseEntity.notFound().build();
        if (match.getTournament() == null) return ResponseEntity.badRequest().body("No tournament");

        Long tournamentId = match.getTournament().getId();
        Team team1 = match.getTeam1();
        Team team2 = match.getTeam2();
        Team winner = match.getWinnerTeam(); // null = draw

        PtsTable pts1 = getOrCreate(tournamentId, team1, match.getTournament());
        PtsTable pts2 = getOrCreate(tournamentId, team2, match.getTournament());

        pts1.setPlayed(safe(pts1.getPlayed()) + 1);
        pts2.setPlayed(safe(pts2.getPlayed()) + 1);

        if (winner == null) {
            pts1.setDraws(safe(pts1.getDraws()) + 1);
            pts2.setDraws(safe(pts2.getDraws()) + 1);
            pts1.setPoints(safe(pts1.getPoints()) + 1);
            pts2.setPoints(safe(pts2.getPoints()) + 1);
        } else {
            PtsTable ptsWinner = winner.getId().equals(team1.getId()) ? pts1 : pts2;
            PtsTable ptsLoser  = winner.getId().equals(team1.getId()) ? pts2 : pts1;
            ptsWinner.setWins(safe(ptsWinner.getWins()) + 1);
            ptsLoser.setLosses(safe(ptsLoser.getLosses()) + 1);
            ptsWinner.setPoints(safe(ptsWinner.getPoints()) + 2);
        }

        ptsTableInterface.save(pts1);
        ptsTableInterface.save(pts2);

        return ResponseEntity.ok(Map.of("message", "Points table updated"));
    }

    public void updateGoalData(Long matchId, int team1Score, int team2Score) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null) return;
        Long tournamentId = match.getTournament().getId();

        PtsTable pts1 = ptsTableInterface.findByTournamentIdAndTeamId(tournamentId, match.getTeam1().getId());
        PtsTable pts2 = ptsTableInterface.findByTournamentIdAndTeamId(tournamentId, match.getTeam2().getId());

        if (pts1 != null) {
            pts1.setGoalsFor(safe(pts1.getGoalsFor()) + team1Score);
            pts1.setGoalsAgainst(safe(pts1.getGoalsAgainst()) + team2Score);
            ptsTableInterface.save(pts1);
        }
        if (pts2 != null) {
            pts2.setGoalsFor(safe(pts2.getGoalsFor()) + team2Score);
            pts2.setGoalsAgainst(safe(pts2.getGoalsAgainst()) + team1Score);
            ptsTableInterface.save(pts2);
        }
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
                    dto.setDraws(safe(pt.getDraws()));
                    dto.setLosses(safe(pt.getLosses()));
                    dto.setPoints(safe(pt.getPoints()));
                    dto.setGoalsFor(safe(pt.getGoalsFor()));
                    dto.setGoalsAgainst(safe(pt.getGoalsAgainst()));
                    dto.setGoalDifference(safe(pt.getGoalsFor()) - safe(pt.getGoalsAgainst()));
                    dto.setSport("futsal");
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