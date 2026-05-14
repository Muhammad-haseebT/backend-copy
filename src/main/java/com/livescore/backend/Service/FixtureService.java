package com.livescore.backend.Service;

import com.livescore.backend.DTO.FixtureRequestDTO;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Team;
import com.livescore.backend.Entity.Tournament;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.TeamInterface;
import com.livescore.backend.Interface.TournamentInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FixtureService {
    @Autowired
    private TournamentInterface tournamentRepository;
    
    @Autowired
    private MatchInterface matchRepository;

    @Autowired
    private TeamInterface teamRepository;

    public ResponseEntity<?> generateFixtures(Long tournamentId, FixtureRequestDTO request) {
        Optional<Tournament> tOpt = tournamentRepository.findById(tournamentId);
        if (tOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Tournament not found");
        }
        Tournament tournament = tOpt.get();

        List<Match> existingMatches = tournament.getMatches();
        if (existingMatches != null && !existingMatches.isEmpty()) {
            return ResponseEntity.badRequest().body("Fixtures already generated for this tournament");
        }

        List<Team> teams = new ArrayList<>(tournament.getTeams());
        if (teams.size() < 2) {
            return ResponseEntity.badRequest().body("At least 2 teams are required to generate fixtures");
        }

        String type = request.getTournamentType();
        if (type == null || type.isBlank()) {
            type = mapTournamentStageToType(tournament.getTournamentStage());
        }

        List<Match> generatedMatches = new ArrayList<>();

        switch (type) {
            case "ROUND_ROBIN":
                generatedMatches = generateRoundRobin(teams, tournament, request, false);
                break;
            case "LEAGUE":
                generatedMatches = generateRoundRobin(teams, tournament, request, true);
                break;
            case "KNOCK_OUT":
                generatedMatches = generateKnockOut(teams, tournament, request);
                break;
            case "MIXED":
                generatedMatches = generateMixed(teams, tournament, request);
                break;
            default:
                return ResponseEntity.badRequest().body("Invalid tournament type for fixture generation");
        }

        matchRepository.saveAll(generatedMatches);
        
        if ("MIXED".equals(type)) {
            teamRepository.saveAll(teams);
        }

        return ResponseEntity.ok(generatedMatches);
    }

    private String mapTournamentStageToType(String stage) {
        if (stage == null) return "ROUND_ROBIN";
        switch (stage) {
            case "roundRobin": return "ROUND_ROBIN";
            case "league": return "LEAGUE";
            case "knockOut": return "KNOCK_OUT";
            case "roundRobinKnockout": return "MIXED";
            default: return "ROUND_ROBIN";
        }
    }

    private List<Match> generateRoundRobin(List<Team> teams, Tournament tournament, FixtureRequestDTO request, boolean doubleRound) {
        List<Match> matches = new ArrayList<>();
        List<Team> activeTeams = new ArrayList<>(teams);
        if (activeTeams.size() % 2 != 0) {
            activeTeams.add(null);
        }

        int numDays = activeTeams.size() - 1;
        int halfSize = activeTeams.size() / 2;

        LocalDate currentDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalTime currentTime = request.getStartTime() != null ? request.getStartTime() : LocalTime.of(9, 0);
        int gapMinutes = request.getGapMinutes() != null ? request.getGapMinutes() : 120;

        int roundCounter = 1;
        int matchCounter = 0;

        for (int pass = 0; pass < (doubleRound ? 2 : 1); pass++) {
            for (int day = 0; day < numDays; day++) {
                for (int idx = 0; idx < halfSize; idx++) {
                    Team team1 = activeTeams.get(idx);
                    Team team2 = activeTeams.get(activeTeams.size() - 1 - idx);

                    if (team1 != null && team2 != null) {
                        if (pass == 1) {
                            Team temp = team1;
                            team1 = team2;
                            team2 = temp;
                        }

                        Match match = createMatchBase(team1, team2, tournament, request);
                        match.setRoundNumber(roundCounter);
                        
                        scheduleMatch(match, currentDate, currentTime, matchCounter, gapMinutes);
                        matches.add(match);
                        matchCounter++;
                    }
                }
                roundCounter++;
                activeTeams.add(1, activeTeams.remove(activeTeams.size() - 1));
            }
        }
        return matches;
    }

    private List<Match> generateKnockOut(List<Team> teams, Tournament tournament, FixtureRequestDTO request) {
        List<Match> matches = new ArrayList<>();
        int power = 1;
        while (power < teams.size()) {
            power *= 2;
        }

        List<Team> paddedTeams = new ArrayList<>(teams);
        while (paddedTeams.size() < power) {
            paddedTeams.add(null);
        }

        LocalDate currentDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalTime currentTime = request.getStartTime() != null ? request.getStartTime() : LocalTime.of(9, 0);
        int gapMinutes = request.getGapMinutes() != null ? request.getGapMinutes() : 120;
        int matchCounter = 0;

        int half = power / 2;
        for (int i = 0; i < half; i++) {
            Team team1 = paddedTeams.get(i);
            Team team2 = paddedTeams.get(power - 1 - i);

            if (team1 == null && team2 == null) continue;

            Match match = createMatchBase(team1, team2, tournament, request);
            match.setRoundNumber(1);
            scheduleMatch(match, currentDate, currentTime, matchCounter, gapMinutes);
            matches.add(match);
            matchCounter++;
        }

        return matches;
    }

    private List<Match> generateMixed(List<Team> teams, Tournament tournament, FixtureRequestDTO request) {
        List<Match> matches = new ArrayList<>();
        
        int groupSize = 4;
        int numGroups = (int) Math.ceil((double) teams.size() / groupSize);
        
        LocalDate currentDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        LocalTime currentTime = request.getStartTime() != null ? request.getStartTime() : LocalTime.of(9, 0);
        int gapMinutes = request.getGapMinutes() != null ? request.getGapMinutes() : 120;
        int matchCounter = 0;

        for (int i = 0; i < numGroups; i++) {
            String groupName = "Group " + (char)('A' + i);
            int startIdx = i * groupSize;
            int endIdx = Math.min(startIdx + groupSize, teams.size());
            List<Team> groupTeams = teams.subList(startIdx, endIdx);
            
            for (Team t : groupTeams) {
                t.setGroupName(groupName);
            }
            
            List<Team> activeTeams = new ArrayList<>(groupTeams);
            if (activeTeams.size() % 2 != 0) {
                activeTeams.add(null);
            }
            
            int numDays = activeTeams.size() - 1;
            int halfSize = activeTeams.size() / 2;
            int roundCounter = 1;

            for (int day = 0; day < numDays; day++) {
                for (int idx = 0; idx < halfSize; idx++) {
                    Team team1 = activeTeams.get(idx);
                    Team team2 = activeTeams.get(activeTeams.size() - 1 - idx);

                    if (team1 != null && team2 != null) {
                        Match match = createMatchBase(team1, team2, tournament, request);
                        match.setRoundNumber(roundCounter);
                        match.setGroupName(groupName);
                        scheduleMatch(match, currentDate, currentTime, matchCounter, gapMinutes);
                        matches.add(match);
                        matchCounter++;
                    }
                }
                roundCounter++;
                activeTeams.add(1, activeTeams.remove(activeTeams.size() - 1));
            }
        }
        
        return matches;
    }

    private Match createMatchBase(Team team1, Team team2, Tournament tournament, FixtureRequestDTO request) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setTeam1(team1);
        match.setTeam2(team2);
        match.setStatus("UPCOMING");
        match.setVenue(request.getVenue());
        if (request.getOvers() != null) {
            match.setOvers(request.getOvers());
        }
        return match;
    }

    private void scheduleMatch(Match match, LocalDate startDate, LocalTime startTime, int matchOffset, int gapMinutes) {
        int matchesPerDay = (8 * 60) / gapMinutes;
        if(matchesPerDay == 0) matchesPerDay = 1;
        
        int dayOffset = matchOffset / matchesPerDay;
        int matchInDay = matchOffset % matchesPerDay;
        
        match.setDate(startDate.plusDays(dayOffset));
        match.setTime(startTime.plusMinutes((long) matchInDay * gapMinutes));
    }
}
