package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Service.SportsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SportsController {
    @Autowired
    private SportsService sportsService;

    @PostMapping("/sports")
    public ResponseEntity<?> createSports(@RequestBody Sports sports) {
        return sportsService.createSports(sports);
    }

    @GetMapping("/sports/{id}")
    public ResponseEntity<?> getSportsById(@PathVariable Long id) {
        return sportsService.getSportsById(id);
    }

    @GetMapping("/sports")
    public ResponseEntity<?> getAllSports() {
        return sportsService.getAllSports();
    }

    @PutMapping("/sports/{id}")
    public ResponseEntity<?> updateSports(@PathVariable Long id, @RequestBody Sports sports) {
        return sportsService.updateSports(id, sports);
    }

    @DeleteMapping("/sports/{id}")
    public ResponseEntity<?> deleteSports(@PathVariable Long id) {
        return sportsService.deleteSports(id);
    }
}
