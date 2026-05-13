package com.livescore.backend.Service;

import com.livescore.backend.DTO.SeasonCreateRequestDTO;
import com.livescore.backend.DTO.SeasonResponse;
import com.livescore.backend.DTO.SeasonSportsRequestDTO;
import com.livescore.backend.Entity.Season;
import com.livescore.backend.Entity.Sports;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.AccountInterface;
import com.livescore.backend.Interface.SeasonInterface;
import com.livescore.backend.Interface.SportsInterface;
import com.livescore.backend.Interface.TournamentInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SeasonService {
    @Autowired
    private SeasonInterface seasonInterface;
    @Autowired
    private AccountInterface accountInterface;
    @Autowired
    private TournamentInterface tournamentInterface;
    @Autowired
    private SportsInterface sportsInterface;

    @Caching(evict = {
            @CacheEvict(value = "seasons", allEntries = true),
            @CacheEvict(value = "seasonById", allEntries = true),
            @CacheEvict(value = "SeasonNames", allEntries = true),
            @CacheEvict(value = "allSeasons", allEntries = true)
    })
    public ResponseEntity<?> createSeason(SeasonCreateRequestDTO season) {
        if (season == null) {
            return ResponseEntity.badRequest().body("Season details are required");
        }
        if (season.getName() == null || season.getName().isBlank()) {
            return ResponseEntity.badRequest().body("Season name is required");
        }
        if (seasonInterface.existsByName(season.getName())) {
            return ResponseEntity.badRequest().body("Season name already exists");
        }
        if (season.getUsername() == null || season.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body("Username is required");
        }
        if (!accountInterface.existsActiveByUsername(season.getUsername())) {
            return ResponseEntity.badRequest().body("User not found");
        }
        var rolecheck = accountInterface.findByUsername(season.getUsername());
        if (rolecheck == null || rolecheck.getRole() == null) {
            return ResponseEntity.badRequest().body("User role not found");
        }
        if (!rolecheck.getRole().equalsIgnoreCase("admin")) {
            return ResponseEntity.badRequest().body("Only admin can create a season");
        }

        Season season1 = new Season();
        season1.setName(season.getName());
        season1.setAccount(rolecheck);


        return ResponseEntity.ok(seasonInterface.save(season1));

    }


    @Cacheable(value="seasonById",key = "#id")
    public ResponseEntity<?> getSeasonById(Long id) {
        if (seasonInterface.findById(id).isPresent()) {
            return ResponseEntity.ok(
                    seasonInterface.findSportWiseTournamentCount(id)
            );
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @Cacheable(value="allSeasons")
    public ResponseEntity<?> getAllSeasons() {
        return ResponseEntity.ok(seasonInterface.findAll());
    }

    @Caching(evict = {
            @CacheEvict(value = "seasons", allEntries = true),
            @CacheEvict(value = "seasonById", allEntries = true),
            @CacheEvict(value = "seasonNames", allEntries = true),
            @CacheEvict(value = "allSeasons", allEntries = true)
    })
    public ResponseEntity<?> updateSeason(Long id, Season season) {
        if (season == null) {
            return ResponseEntity.badRequest().body("Season details are required");
        }
        var opt = seasonInterface.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Season season1 = opt.get();
        if (season.getName() != null && !season.getName().isBlank()) {
            season1.setName(season.getName());
        }
        return ResponseEntity.ok(seasonInterface.save(season1));
    }
    @Caching(evict = {
            @CacheEvict(value = "seasons", allEntries = true),
            @CacheEvict(value = "seasonById", allEntries = true),
            @CacheEvict(value = "SeasonNames", allEntries = true),
            @CacheEvict(value = "allSeasons", allEntries = true)
    })
    public ResponseEntity<?> deleteSeason(Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body("Season id is required");
        }
        if (!seasonInterface.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        seasonInterface.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @Cacheable(value = "SeasonNames")
    public ResponseEntity<?> getSeasonNames() {
        return ResponseEntity.ok(seasonInterface.findAll().stream().map(season->new SeasonResponse(season.getName(),season.getId())).collect(Collectors.toList()));
    }

    @Cacheable(value = "seasons",key = "(T(java.util.Objects).hash(#id , #sportId ))")
    public ResponseEntity<?> getSeasonWiseTournament(Long id,Long sportId) {
        if (id == null || sportId == null) {
            return ResponseEntity.badRequest().body("Season id and sport id are required");
        }
        if (seasonInterface.findById(id).isPresent()) {
            List<SportTournamentCount> s = seasonInterface.findSportWiseTournamentCount(id);

            for (SportTournamentCount sr : s) {
                if (sr != null && sr.getSportId() != null && sr.getSportId().equals(sportId)) {
                    return ResponseEntity.ok(tournamentInterface.findBySeasonIdAndSportName(id, sr.getSportId()));
                }
            }

        }

            return ResponseEntity.notFound().build();


    }


    @Caching(evict = {
            @CacheEvict(value = "seasons", allEntries = true),
            @CacheEvict(value = "seasonById", allEntries = true),
            @CacheEvict(value = "SeasonNames", allEntries = true),
            @CacheEvict(value = "allSeasons", allEntries = true)
    })
    public ResponseEntity<?> addSportsToSeason(SeasonSportsRequestDTO request)
    {
        if (request == null || request.getSeasonId() == null) {
            return ResponseEntity.badRequest().body("Season id is required");
        }
        if (request.getSportsIds() == null || request.getSportsIds().isEmpty()) {
            return ResponseEntity.badRequest().body("Sports ids are required");
        }
        Season season = seasonInterface.findById(request.getSeasonId()).orElse(null);
        if (season == null) {
            return ResponseEntity.badRequest().body("Season not found");
        }

        List<Sports> sportsList = sportsInterface.findAllById(request.getSportsIds());
        if (sportsList.isEmpty()) {
            return ResponseEntity.badRequest().body("No valid sports found for given IDs");
        }

        if (season.getSportsOffered() == null) {
            season.setSportsOffered(new ArrayList<>());
        }

        for (Sports s : sportsList) {
            if (!season.getSportsOffered().contains(s)) {
                season.getSportsOffered().add(s);
            }
        }

       return ResponseEntity.ok(seasonInterface.save(season));
    }
}
