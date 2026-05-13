package com.livescore.backend.Controller;

import com.livescore.backend.Sport.Badminton.BadmintonPtsTableService;
import com.livescore.backend.Entity.PtsTable;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Sport.Chess.ChessPtsTableService;
import com.livescore.backend.Sport.Futsal.FutsalPtsTableService;
import com.livescore.backend.Interface.TournamentInterface;
import com.livescore.backend.Service.PtsTableService;
import com.livescore.backend.Sport.Ludo.LudoPtsTableService;
import com.livescore.backend.Sport.TableTennis.TableTennisPtsTableService;
import com.livescore.backend.Sport.TugOfWar.TugOfWarPtsTableService;
import com.livescore.backend.Sport.Volleyball.VolleyballPtsTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PtsTableController {

    private final PtsTableService           ptsTableService;
    private final FutsalPtsTableService     futsalPtsTableService;
    private final VolleyballPtsTableService  volleyballPtsTableService;
    private final BadmintonPtsTableService   badmintonPtsTableService;
    private final TournamentInterface        tournamentInterface;
    private final TableTennisPtsTableService tableTennisPtsTableService;
    private final TugOfWarPtsTableService tugOfWarPtsTableService;
    private final LudoPtsTableService ludoPtsTableService;
    private final ChessPtsTableService chessPtsTableService;

    @PostMapping("/ptsTable")
    public ResponseEntity<?> createPtsTable(@RequestBody PtsTable ptsTable) {
        return ptsTableService.createPtsTable(ptsTable);
    }

    @PutMapping("/ptsTable/{id}")
    public ResponseEntity<?> updatePtsTable(@PathVariable Long id) {
        return ptsTableService.updatePointsTableAfterMatch(id);
    }

    @DeleteMapping("/ptsTable/{id}")
    public ResponseEntity<?> deletePtsTable(@PathVariable Long id) {
        return ptsTableService.deletePtsTable(id);
    }

    @GetMapping("/ptsTable")
    public ResponseEntity<?> getAllPtsTables() { return ptsTableService.getAllPtsTables(); }

    @GetMapping("/ptsTable/{id}")
    public ResponseEntity<?> getPtsTableById(@PathVariable Long id) {
        return ptsTableService.getPtsTableById(id);
    }

    @GetMapping("/ptsTable/tournament/{tournamentId}")
    public ResponseEntity<?> getPtsTablesByTournament(@PathVariable Long tournamentId) {
        return ptsTableService.getPtsTablesByTournament(tournamentId);
    }

    @GetMapping("/tournament/{tournamentId}/points")
    public ResponseEntity<?> getPointsTable(@PathVariable Long tournamentId) {
        Tournament t = tournamentInterface.findById(tournamentId).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        String sport = t.getSport() != null ? t.getSport().getName().toLowerCase() : "cricket";
        return switch (sport) {
            case "futsal"     -> futsalPtsTableService.getTable(tournamentId);
            case "volleyball" -> volleyballPtsTableService.getTable(tournamentId);
            case "badminton"  -> badmintonPtsTableService.getTable(tournamentId);
            case "table tennis",
                 "tabletennis"   -> tableTennisPtsTableService.getTable(tournamentId);
            case "tug of war","tugofwar"         -> tugOfWarPtsTableService.getTable(tournamentId);
            case "ludo"                        -> ludoPtsTableService.getTable(tournamentId);
            case "chess"                      -> chessPtsTableService.getTable(tournamentId);
            default           -> ptsTableService.getPtsTablesByTournament(tournamentId);
        };
    }
}