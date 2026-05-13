package com.livescore.backend.Sport.Ludo;

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
public class LudoPtsTableService {

    private final PtsTableInterface ptsTableInterface;
    private final MatchInterface    matchInterface;

    @Transactional
    public void updateAfterMatch(Long matchId) {
        Match match = matchInterface.findById(matchId).orElse(null);
        if (match == null || match.getTournament() == null || match.getWinnerTeam() == null) return;
        Long tid = match.getTournament().getId();
        PtsTable pts1 = getOrCreate(tid, match.getTeam1(), match.getTournament());
        PtsTable pts2 = getOrCreate(tid, match.getTeam2(), match.getTournament());
        pts1.setPlayed(safe(pts1.getPlayed()) + 1);
        pts2.setPlayed(safe(pts2.getPlayed()) + 1);
        boolean t1Won = match.getWinnerTeam().getId().equals(match.getTeam1().getId());
        if (t1Won) { pts1.setWins(safe(pts1.getWins())+1); pts1.setPoints(safe(pts1.getPoints())+2); pts2.setLosses(safe(pts2.getLosses())+1); }
        else       { pts2.setWins(safe(pts2.getWins())+1); pts2.setPoints(safe(pts2.getPoints())+2); pts1.setLosses(safe(pts1.getLosses())+1); }
        ptsTableInterface.save(pts1); ptsTableInterface.save(pts2);
    }

    public ResponseEntity<?> getTable(Long tournamentId) {
        List<PtsTable> tables = ptsTableInterface.findByTournamentId(tournamentId);
        List<PtsTableDTO> dtos = tables.stream().map(pt -> {
            PtsTableDTO dto = new PtsTableDTO();
            dto.setId(pt.getId()); dto.setTeamId(pt.getTeam().getId()); dto.setTeamName(pt.getTeam().getName());
            dto.setTournamentId(tournamentId); dto.setPlayed(safe(pt.getPlayed()));
            dto.setWins(safe(pt.getWins())); dto.setLosses(safe(pt.getLosses())); dto.setPoints(safe(pt.getPoints()));
            return dto;
        }).sorted(Comparator.comparingInt(PtsTableDTO::getPoints).reversed()
                .thenComparingInt(PtsTableDTO::getWins).reversed())
        .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private PtsTable getOrCreate(Long tid, Team team, Tournament tournament) {
        PtsTable e = ptsTableInterface.findByTournamentIdAndTeamId(tid, team.getId());
        if (e != null) return e;
        PtsTable p = new PtsTable(); p.setTeam(team); p.setTournament(tournament);
        p.setPlayed(0); p.setWins(0); p.setLosses(0); p.setPoints(0);
        return ptsTableInterface.save(p);
    }
    private int safe(Integer v) { return v == null ? 0 : v; }
}
