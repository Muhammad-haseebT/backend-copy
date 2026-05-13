package com.livescore.backend.Service;

import com.livescore.backend.DTO.TournamentRequestDTO;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Entity.Season;
import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.*;
import com.livescore.backend.Util.Constants;
import com.livescore.backend.Util.ValidationUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TournamentService {
    @Autowired
    private TournamentInterface tournamentInterface;
    @Autowired
    private AccountInterface accountInterface;
    @Autowired
    private SportsInterface sportsInterface;
    @Autowired
    private SeasonInterface seasonInterface;
    @Autowired
    private PtsTableInterface ptsTableInterface;
    @Autowired
    private PlayerRequestInterface playerRequestInterface;
    @Autowired
    private PlayerInterface playerInterface;


    @CacheEvict(value = {"tournamentOverview","tournamentNames","tournamentById","tournaments","seasons","seasonById"},allEntries = true)
    public ResponseEntity<?> createTournament(TournamentRequestDTO tournament) {
        // Validate input
        ResponseEntity<?> validation = ValidationUtils.validateNotNull(tournament, "Tournament details");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequired(tournament.getName(), "Tournament name");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequiredId(tournament.getSeasonId(), "Season id");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequiredId(tournament.getSportsId(), "Sports id");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequired(tournament.getUsername(), "Username");

        if (validation != null) return validation;
        // Check for duplicates and existing references
        if (tournamentInterface.existsByNameAndSeasonId(tournament.getName(), tournament.getSeasonId())) {
            return ValidationUtils.badRequest("Tournament name already exists");
        }
        if (!accountInterface.existsActiveByUsername(tournament.getUsername())) {
            return ValidationUtils.badRequest("User not found");
        }
        if (!sportsInterface.existsById(tournament.getSportsId())) {
            return ValidationUtils.badRequest("Sports not found");
        }
        if (!seasonInterface.existsById(tournament.getSeasonId())) {
            return ValidationUtils.badRequest("Season not found");
        }

        Tournament tournament1=new Tournament();
        tournament1.setName(tournament.getName());
        
        // Check user role
        var organizer = accountInterface.findByUsername(tournament.getUsername());
        if (organizer == null || organizer.getRole() == null) {
            return ValidationUtils.badRequest("User role not found");
        }
        String role = organizer.getRole().trim().toUpperCase();
        if (!role.equals(Constants.ROLE_ADMIN)) {
            return ValidationUtils.badRequest("Only admin can create a tournament");
        }


        tournament1.setOrganizer(organizer);
        Season season = seasonInterface.findById(tournament.getSeasonId()).orElse(null);
        if (season == null) {
            return ValidationUtils.badRequest("Season not found");
        }
        Sports sport = sportsInterface.findById(tournament.getSportsId()).orElse(null);
        if (sport == null) {
            return ValidationUtils.badRequest("Sport not found");
        }
        tournament1.setSeason(season);
        tournament1.setSport(sport);
        tournament1.setStartDate(tournament.getStartDate());
        tournament1.setEndDate(tournament.getEndDate());
        tournament1.setPlayerType(tournament.getPlayerType());
        tournament1.setTournamentType(tournament.getTournamentType());
        tournament1.setTournamentStage(tournament.getTournamentStage());

       tournamentInterface.save(tournament1);

        if (season.getSportsOffered() == null) {
            season.setSportsOffered(new ArrayList<>());
        }

        if (!season.getSportsOffered().contains(sport)) {
            season.getSportsOffered().add(sport);
            seasonInterface.save(season);
        }



        return ResponseEntity.ok().build();

    }

    @Cacheable(value = "tournamentById",key = "#id")
    public ResponseEntity<?> getTournamentById(Long id) {
        return tournamentInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



    @Cacheable(value = "tournaments")
    public ResponseEntity<?> getAllTournaments() {
        return ResponseEntity.ok(tournamentInterface.findAll());
    }
    @CacheEvict(value = {"tournamentOverview","tournamentNames","tournamentById","tournaments","seasons"},allEntries = true)
    public ResponseEntity<?> updateTournament(Long id, TournamentRequestDTO tournament) {
        ResponseEntity<?> validation = ValidationUtils.validateNotNull(tournament, "Tournament details");
        if (validation != null) return validation;
        var opt = tournamentInterface.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Tournament tournament1 = opt.get();

        if (tournament.getName() != null && !tournament.getName().isBlank()) {
            tournament1.setName(tournament.getName());
        }
        if (tournament.getUsername() != null && !tournament.getUsername().isBlank()) {
            var organizer = accountInterface.findByUsername(tournament.getUsername());
            if (organizer == null) {
                return ValidationUtils.badRequest("User not found");
            }
            tournament1.setOrganizer(organizer);
        }
        if (tournament.getSeasonId() != null) {
            Season season = seasonInterface.findById(tournament.getSeasonId()).orElse(null);
            if (season == null) {
                return ValidationUtils.badRequest("Season not found");
            }
            tournament1.setSeason(season);
        }
        if (tournament.getSportsId() != null) {
            Sports sport = sportsInterface.findById(tournament.getSportsId()).orElse(null);
            if (sport == null) {
                return ValidationUtils.badRequest("Sport not found");
            }
            tournament1.setSport(sport);
        }
        if (tournament.getStartDate() != null) {
            tournament1.setStartDate(tournament.getStartDate());
        }
        if (tournament.getEndDate() != null) {
            tournament1.setEndDate(tournament.getEndDate());
        }
        if (tournament.getPlayerType() != null) {
            tournament1.setPlayerType(tournament.getPlayerType());
        }
        if (tournament.getTournamentType() != null) {
            tournament1.setTournamentType(tournament.getTournamentType());
        }
        if (tournament.getTournamentStage() != null) {
            tournament1.setTournamentStage(tournament.getTournamentStage());
        }

        return ResponseEntity.ok(tournamentInterface.save(tournament1));
    }
    @CacheEvict(value = {"tournamentOverview","tournamentNames","tournamentById","tournaments","seasons"},allEntries = true)

    public ResponseEntity<?> deleteTournament(Long id) {
        if(tournamentInterface.existsById(id)){
            tournamentInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.notFound().build();
        }
    }


    @Cacheable(value = "tournamentOverview",key = "#id")
    public ResponseEntity<?> getOverview(Long id) {
        Tournament tournament = tournamentInterface.findById(id).orElse(null);
        if(tournament == null){
            return ResponseEntity.notFound().build();
        }
        OverViewDTO overViewDTO = new OverViewDTO();
        if (tournament.getTeams() == null) {
            overViewDTO.setTeams(0);
        } else {
            overViewDTO.setTeams((int) tournament.getTeams().stream().filter(team -> team != null && "APPROVED".equals(team.getStatus())).count());
        }
        overViewDTO.setPlayerType(tournament.getPlayerType());
        overViewDTO.setStartDate(tournament.getStartDate());
        Pageable p=PageRequest.of(0, 3);
        List<Abc> a= ptsTableInterface.findByTournamentId(id,p);
        overViewDTO.setTop(a == null ? List.of() : a);
        return ResponseEntity.ok(overViewDTO);

    }

    @Cacheable(value = "tournamentNames")
    public ResponseEntity<?> getTournamentByName() {
        List<Map<Long,String>> t =new ArrayList<>();
        List<Tournament>at=tournamentInterface.findAllNames();
        for (Tournament tournament : at) {
            Map<Long,String> mp=new HashMap<>();
            mp.put(tournament.getId(),tournament.getName());
            t.add(mp);
        }


        return ResponseEntity.ok(t);
    }
}

@Data
class OverViewDTO {
    private int teams;
    private String playerType;
    private LocalDate startDate;
    List<Abc> top;
}
