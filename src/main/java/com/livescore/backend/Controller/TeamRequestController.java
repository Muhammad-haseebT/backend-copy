package com.livescore.backend.Controller;

import com.livescore.backend.DTO.TeamRequestDTO;
import com.livescore.backend.Service.TeamRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeamRequestController {
    @Autowired
    private TeamRequestService teamRequestService;
    @PostMapping("/teamRequest")
    public ResponseEntity<?> createTeamRequest(@RequestBody TeamRequestDTO teamRequest) {
        return teamRequestService.createTeamRequest(teamRequest);
    }
    @PutMapping("/teamRequest/{id}")
    public ResponseEntity<?> updateTeamRequest(@PathVariable Long id, @RequestBody TeamRequestDTO teamRequest) {
        return teamRequestService.updateTeamRequest(id, teamRequest);
    }
    @DeleteMapping("/teamRequest/{id}")
    public ResponseEntity<?> deleteTeamRequest(@PathVariable Long id) {
        return teamRequestService.deleteTeamRequest(id);
    }

    @GetMapping("/teamRequest/{id}")
    public ResponseEntity<?> getTeamRequestById(@PathVariable Long id) {
        return teamRequestService.getTeamRequestById(id);
    }
    //approveRequest
    @PutMapping("/teamRequest/approve/{id}")
    public ResponseEntity<?> approveTeamRequest(@PathVariable Long id) {
        return teamRequestService.approveTeamRequest(id);
    }
    //rejectRequest
    @PutMapping("/teamRequest/reject/{id}")
    public ResponseEntity<?> rejectTeamRequest(@PathVariable Long id) {
        return teamRequestService.rejectTeamRequest(id);
    }

    @GetMapping("/teamRequest")
    public ResponseEntity<?> getTeamRequestsByTournamentId() {
        return teamRequestService.getTeamRequestsByTournamentId();
    }

}
