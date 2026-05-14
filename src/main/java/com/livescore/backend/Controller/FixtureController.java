package com.livescore.backend.Controller;

import com.livescore.backend.DTO.FixtureRequestDTO;
import com.livescore.backend.Service.FixtureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class FixtureController {
    
    @Autowired
    private FixtureService fixtureService;

    @PostMapping("/tournament/{id}/generate-fixtures")
    public ResponseEntity<?> generateFixtures(@PathVariable Long id, @RequestBody(required = false) FixtureRequestDTO request) {
        if(request == null) {
            request = new FixtureRequestDTO();
        }
        return fixtureService.generateFixtures(id, request);
    }
}
