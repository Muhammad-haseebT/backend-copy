package com.livescore.backend.Controller;

import com.livescore.backend.DTO.TournamentRequestDTO;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Service.TournamentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
public class TournamentController {
    @Autowired
    private TournamentService tournamentService;

    @PostMapping("/tournament")
    public ResponseEntity<?> createTournament(@RequestBody TournamentRequestDTO tournament) {
        return tournamentService.createTournament(tournament);
    }

    @GetMapping("/tournament/{id}")
    public ResponseEntity<?> getTournamentById(@PathVariable Long id) {
        return tournamentService.getTournamentById(id);
    }

    @GetMapping("/tournament")
    public ResponseEntity<?> getAllTournaments() {
        return tournamentService.getAllTournaments();
    }

    @PutMapping("/tournament/{id}")
    public ResponseEntity<?> updateTournament(@PathVariable Long id, @RequestBody TournamentRequestDTO tournament) {
        return tournamentService.updateTournament(id, tournament);
    }

    @DeleteMapping("/tournament/{id}")
    public ResponseEntity<?> deleteTournament(@PathVariable Long id) {
        return tournamentService.deleteTournament(id);
    }

    @GetMapping("/tournament/overview/{id}")
    public ResponseEntity<?> getOverview(@PathVariable Long id) {
        return tournamentService.getOverview(id);
    }

    @GetMapping("/tournament/namesAndIds")
    public ResponseEntity<?> getNames(){
        return tournamentService.getTournamentByName();
    }



}
