package com.livescore.backend.Service;

import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Interface.SportsInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SportsService {
    @Autowired
    private SportsInterface sportsInterface;

    public ResponseEntity<?> createSports(Sports sports) {
        if (sports == null) {
            return ResponseEntity.badRequest().body("Sports details are required");
        }
        if (sports.getName() == null || sports.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Sports name is required");
        }
        if (sportsInterface.existsByName(sports.getName())) {
            return ResponseEntity.badRequest().body("Sports name already exists");
        }
        return ResponseEntity.ok(sportsInterface.save(sports));
    }

    public ResponseEntity<?> getSportsById(Long id) {
        return sportsInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> getAllSports() {
        return ResponseEntity.ok(sportsInterface.findAll());
    }

    public ResponseEntity<?> updateSports(Long id, Sports sports) {
        if (sports == null) {
            return ResponseEntity.badRequest().body("Sports details are required");
        }
        var opt = sportsInterface.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Sports sports1 = opt.get();
        if (sports.getName() != null && !sports.getName().isBlank()) {
            sports1.setName(sports.getName());
        }
        return ResponseEntity.ok(sportsInterface.save(sports1));
    }

    public ResponseEntity<?> deleteSports(Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("Sports id is required");
        }
        if (!sportsInterface.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        sportsInterface.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
