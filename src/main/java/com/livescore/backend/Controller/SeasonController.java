package com.livescore.backend.Controller;

import com.livescore.backend.DTO.SeasonCreateRequestDTO;
import com.livescore.backend.DTO.SeasonSportsRequestDTO;
import com.livescore.backend.Entity.Season;
import com.livescore.backend.Service.SeasonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SeasonController {
    @Autowired
    private SeasonService seasonService;

    @PostMapping("/season")
    public ResponseEntity<?> createSeason(@RequestBody SeasonCreateRequestDTO season) {
        return seasonService.createSeason(season);
    }

    @GetMapping("/season/{id}")
    public ResponseEntity<?> getSeasonById(@PathVariable Long id) {
        return seasonService.getSeasonById(id);
    }
    @GetMapping("/season/tournaments/{id}/{sid}")
    public ResponseEntity<?> getSeasonWiseTournament(@PathVariable Long id,@PathVariable Long sid) {
        return seasonService.getSeasonWiseTournament(id,sid);
    }

    @GetMapping("/season")
    public ResponseEntity<?> getAllSeasons() {
        return seasonService.getAllSeasons();
    }

    @PutMapping("/season/{id}")
    public ResponseEntity<?> updateSeason(@PathVariable Long id,@RequestBody Season season) {
        return seasonService.updateSeason(id, season);
    }

    @DeleteMapping("/season/{id}")
    public ResponseEntity<?> deleteSeason(@PathVariable Long id) {
        return seasonService.deleteSeason(id);
    }
    @GetMapping("/season/names")
    public ResponseEntity<?> getSeasonNames() {
        return seasonService.getSeasonNames();
    }

    @PostMapping("/add-sports")
    public ResponseEntity<?> addSportsToSeason(@RequestBody SeasonSportsRequestDTO request) {
        return seasonService.addSportsToSeason(request);
    }

}
