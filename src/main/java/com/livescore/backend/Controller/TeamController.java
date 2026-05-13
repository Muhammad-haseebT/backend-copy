package com.livescore.backend.Controller;

import com.livescore.backend.Entity.Team;
import com.livescore.backend.Service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeamController {
    @Autowired
    private TeamService teamService;

    @PostMapping("/team/{id}/{playerId}")
    public ResponseEntity<?> createTeam(@RequestBody Team team,@PathVariable Long id,@PathVariable Long playerId) {
        return teamService.createTeam(team,id,playerId);
    }
    @PostMapping("/team/u/{id}")
    public ResponseEntity<?> updateTeam(@PathVariable Long id, @RequestBody Team team) {
        return teamService.updateTeam(id, team);
    }
    @DeleteMapping("/team/{id}")
    public ResponseEntity<?> deleteTeam(@PathVariable Long id) {
        return teamService.deleteTeam(id);
    }
    @GetMapping("/team")
    public ResponseEntity<?> getAllTeams() {
        return teamService.getAllTeams();
    }
    @GetMapping("/team/{id}")
    public ResponseEntity<?> getTeamById(@PathVariable Long id) {
        return teamService.getTeamById(id);
    }
    @GetMapping("/team/tournament/{tid}")
    public ResponseEntity<?> getTeamByTournamentId(@PathVariable Long tid) {
        return teamService.getTeamByTournamentId( tid);
    }
    @GetMapping("/team/tournament/account/{tid}/{aid}")
    public  ResponseEntity<?>getTeamByTournamentIdAndAccountId(@PathVariable Long tid,@PathVariable Long aid){
        return teamService.getTeamByTournamentIdAndAccountId(tid,aid);
    }
    //;get teams players
    @GetMapping("/team/{teamId}/players")
    public ResponseEntity<?> getPlayersByTeam(@PathVariable Long teamId)
    {
        return teamService.findPlayersByTeam(teamId);

    }



}
