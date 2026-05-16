package com.livescore.backend.Service;

import com.livescore.backend.DTO.TeamStatsResponseDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import com.livescore.backend.Util.Constants;
import com.livescore.backend.Util.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamService {
    @Autowired
    private TeamInterface teamInterface;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private PlayerInterface playerInterface;
    @Autowired
    private PlayerRequestInterface pri;

    @Autowired
    private TournamentInterface tournamentInterface;
    @Autowired
    private PlayerRequestInterface playerRequestInterface;

    @Autowired
    private PtsTableInterface ptsTableInterface;

    @Autowired
    private StatsInterface statsInterface;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MatchInterface matchInterface;


    @CacheEvict(value = {"teamByTournamentId", "teams", "teamById", "teamByTournamentIdAndAccountId", "teamByPlayers"}, allEntries = true)
    @Transactional
    public ResponseEntity<?> createTeam(Team team, Long tournamentId, Long playerId) {
        // Validate input
        ResponseEntity<?> validation = ValidationUtils.validateNotNull(team, "Team details");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequiredId(tournamentId, "Tournament id");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequiredId(playerId, "Player id");
        if (validation != null) return validation;

        validation = ValidationUtils.validateRequired(team.getName(), "Team name");
        if (validation != null) return validation;
        Optional<Tournament> tournamentOpt = tournamentInterface.findById(tournamentId);
        if (tournamentOpt.isEmpty()) {
            return ValidationUtils.badRequest("Tournament not found with ID: " + tournamentId);
        }
        Optional<Player> playerOpt = playerInterface.findActiveById(playerId);
        if (playerOpt.isEmpty()) {
            return ValidationUtils.badRequest("Player not found with ID: " + playerId);
        }

        if(playerRequestInterface.findExistingRequest(playerId,tournamentId)!=null){
            return ValidationUtils.badRequest("Player already in an other team");
        }
        Player p1 = playerOpt.get();
        team.setTournament(tournamentOpt.get());
        team.setCreator(p1);
        List<Player> players = teamInterface.findPlayersByteamId(team.getId());
        players.add(p1);
        team.setPlayers(players);
        Team savedTeam = teamInterface.save(team);
        p1.setPlayerRole(Constants.ROLE_CAPTAIN);
        playerInterface.save(p1);


        PlayerRequest playerRequest = new PlayerRequest();
        playerRequest.setPlayer(playerOpt.get());
        playerRequest.setTeam(savedTeam);
        playerRequest.setTournament(tournamentOpt.get());
        playerRequest.setStatus(Constants.STATUS_APPROVED);
        pri.save(playerRequest);

        return ResponseEntity.ok(Map.of(
                "message", "Team created successfully",
                "teamId", savedTeam.getId(),
                "name", savedTeam.getName(),
                "tournamentId", savedTeam.getTournament().getId()
        ));


    }

    @CacheEvict(value = {"teamByTournamentId", "teams", "teamById", "teamByTournamentIdAndAccountId", "teamByPlayers"}, allEntries = true)
    public ResponseEntity<?> updateTeam(Long id, Team team) {
        ResponseEntity<?> validation = ValidationUtils.validateNotNull(team, "Team details");
        if (validation != null) return validation;
        return teamInterface.findById(id).map(teamEntity -> {
            if (team.getName() != null && !team.getName().isBlank()) {
                teamEntity.setName(team.getName());
            }
            if (team.getStatus() != null && !team.getStatus().isBlank()) {
                teamEntity.setStatus(team.getStatus());
            }
            return ResponseEntity.ok(teamInterface.save(teamEntity));
        }).orElse(ResponseEntity.notFound().build());
    }

    @CacheEvict(value = {"teamByTournamentId", "teams", "teamById", "teamByTournamentIdAndAccountId", "teamByPlayers"}, allEntries = true)
    public ResponseEntity<?> deleteTeam(Long id) {
        if (teamInterface.existsById(id)) {
            teamInterface.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Cacheable(value = "teams", key = "#tid")
    public ResponseEntity<?> getAllTeams() {
        return ResponseEntity.ok(teamInterface.findAll());
    }

    @Cacheable(value = "teamById", key = "#tid")

    public ResponseEntity<?> getTeamById(Long id) {
        return teamInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @Cacheable(value = "teamByTournamentId", key = "#tid")
    public ResponseEntity<?> getTeamByTournamentId(Long tid) {
        List<Map<String, Object>> response = new ArrayList<>();
        ResponseEntity<?> validation = ValidationUtils.validateRequiredId(tid, "Tournament id");
        if (validation != null) return validation;
        List<Team> teams = teamInterface.findByTournamentId(tid);
        if (teams == null || teams.isEmpty()) {
            return ResponseEntity.ok(response);
        }

        for (Team team : teams) {
            if (team == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", team.getId());
            m.put("name", team.getName());
            m.put("status", team.getStatus());
            m.put("creatorId", team.getCreator() != null ? team.getCreator().getId() : null);
            response.add(m);
        }

        return ResponseEntity.ok(response);

    }

    @Cacheable(value = "teamByTournamentIdAndAccountId", key = "T(java.util.Objects).hash(#tid,#aid)")

    public ResponseEntity<?> getTeamByTournamentIdAndAccountId(Long tid, Long aid) {
        // Validate input
        if (tid == null || aid == null) {
            return ValidationUtils.badRequest("Tournament id and account id are required");
        }

        // Get player ID
        Long creatorPlayerId = playerInterface.findByAccount_Id(aid)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(Player::getId)
                .orElse(null);

        if (creatorPlayerId == null) {
            return ResponseEntity.notFound().build();
        }

        // Get team
        Optional<Team> teamOpt = teamInterface.findByTournamentIdAndPlayerId(tid, creatorPlayerId);
        if (teamOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Team team = teamOpt.get();

        // OPTIMIZED: Single query with JOIN FETCH
        List<PlayerRequest> players = pri
                .findByTeamIdWithPlayer(team.getId());

        List<Map<String, Object>> playersLite = players.stream()
                .filter(p -> p != null && p.getPlayer() != null)
                .filter(p -> !Boolean.TRUE.equals(p.getPlayer().getIsDeleted()))
                .map(p -> {
                    Map<String, Object> playerMap = new HashMap<>();
                    playerMap.put("id", p.getId());
                    playerMap.put("playerId", p.getPlayer().getId());
                    playerMap.put("name", p.getPlayer().getName());
                    playerMap.put("status", p.getStatus());
                    return playerMap;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("teamName", team.getName());
        response.put("teamId", team.getId());
        response.put("teamStatus", team.getStatus());
        response.put("creatorPlayerId", creatorPlayerId);
        response.put("players", playersLite);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> findPlayersByTeam(Long teamId) {
        Team team = teamInterface.findById(teamId).orElse(null);
        if (team == null) {
            return ResponseEntity.notFound().build();
        }

        Long creatorId = team.getCreator() != null ? team.getCreator().getId() : null;

        // player_request se APPROVED players fetch karo
        List<Player> players = playerRequestInterface.findApprovedPlayersByTeamId(teamId);

        // Return lightweight map with isCreator flag
        List<Map<String, Object>> result = players.stream()
                .filter(p -> p != null && !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    m.put("isCreator", p.getId().equals(creatorId));
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }



    public ResponseEntity<?> getTeamStats(Long teamId) {

        // 1. Team fetch
        Team team = teamInterface.findById(teamId).orElse(null);
        if (team == null) return ResponseEntity.notFound().build();

        Long tournamentId = team.getTournament() != null
                ? team.getTournament().getId() : null;

        TeamStatsResponseDTO dto = new TeamStatsResponseDTO();
        dto.setTeamId(team.getId());
        dto.setTeamName(team.getName());

        // 2. Sport info from tournament
        String sport = "cricket";
        Long sportId = null;
        if (team.getTournament() != null && team.getTournament().getSport() != null) {
            sport   = team.getTournament().getSport().getName().toLowerCase().trim();
            sportId = team.getTournament().getSport().getId();
        }
        dto.setSport(sport);
        dto.setSportId(sportId);

        // 3. Match record from PtsTable
        if (tournamentId != null) {
            PtsTable pts = ptsTableInterface.findByTeamIdAndTournamentId(teamId, tournamentId);
            if (pts != null) {
                dto.setMatchesPlayed(safe(pts.getPlayed()));
                dto.setWins(safe(pts.getWins()));
                dto.setLosses(safe(pts.getLosses()));
                dto.setDraws(safe(pts.getDraws()));
                dto.setNrr(pts.getNrr() != null ? pts.getNrr() : 0.0);
                dto.setGoalsFor(safe(pts.getGoalsFor()));
                dto.setGoalsAgainst(safe(pts.getGoalsAgainst()));
            }

            // Fallback: if PtsTable has no played data, count directly from completed matches
            if (dto.getMatchesPlayed() == 0) {
                long completedCount = matchInterface.findByTournamentId(tournamentId).stream()
                        .filter(m -> "COMPLETED".equalsIgnoreCase(m.getStatus())
                                && m.getTeam1() != null && m.getTeam2() != null
                                && (m.getTeam1().getId().equals(teamId) || m.getTeam2().getId().equals(teamId)))
                        .count();
                dto.setMatchesPlayed((int) completedCount);

                // Count wins by winner team
                if (completedCount > 0) {
                    long wins = matchInterface.findByTournamentId(tournamentId).stream()
                            .filter(m -> "COMPLETED".equalsIgnoreCase(m.getStatus())
                                    && m.getWinnerTeam() != null
                                    && m.getWinnerTeam().getId().equals(teamId))
                            .count();
                    dto.setWins((int) wins);
                    dto.setLosses((int) (completedCount - wins));
                }
            }
        }

        // 4. Approved players in this team
        List<Player> players = playerRequestInterface.findApprovedPlayersByTeamId(teamId);
        if (players == null || players.isEmpty()) {
            return ResponseEntity.ok(dto);  // match record di, player stats nahi
        }

        // 5. Stats rows for those players in this tournament
        List<Long> playerIds = players.stream()
                .filter(p -> p != null && !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(Player::getId)
                .collect(Collectors.toList());

        if (playerIds.isEmpty()) return ResponseEntity.ok(dto);

        List<Stats> statsList = tournamentId != null
                ? statsInterface.findByTournamentIdAndPlayerIds(tournamentId, playerIds)
                : new ArrayList<>();

        // 6. Aggregate and fill DTO
        if (!statsList.isEmpty()) {
            aggregateTeamStats(dto, statsList, sport);
        }

        return ResponseEntity.ok(dto);
    }

    // ── Aggregate all players' Stats into one TeamStatsResponseDTO ────────
    private void aggregateTeamStats(TeamStatsResponseDTO dto,
                                    List<Stats> statsList,
                                    String sport) {
        int totalRuns = 0, totalWickets = 0, totalFours = 0,
                totalSixes = 0, totalCatches = 0, highestIndividual = 0;
        int totalGoals = 0, totalAssists = 0, totalFouls = 0,
                totalYellow = 0, totalRed = 0;

        Stats topScorerStats = null;
        int   topScorerValue = -1;

        for (Stats s : statsList) {
            if (s == null) continue;

            // Cricket fields
            totalRuns    += safe(s.getRuns());
            totalWickets += safe(s.getWickets());
            totalFours   += safe(s.getFours());
            totalSixes   += safe(s.getSixes());
            totalCatches += safe(s.getCatches());
            if (safe(s.getHighest()) > highestIndividual)
                highestIndividual = safe(s.getHighest());

            // Multi-sport reused fields
            totalGoals   += safe(s.getGoals());
            totalAssists += safe(s.getAssists());
            totalFouls   += safe(s.getFouls());
            totalYellow  += safe(s.getYellowCards());
            totalRed     += safe(s.getRedCards());

            // Top scorer — primary key stat per sport
            int keyValue = switch (sport) {
                case "futsal", "volleyball",
                     "badminton", "table tennis", "tabletennis",
                     "ludo", "chess",
                     "tug_of_war", "tug of war" -> safe(s.getGoals());
                default                          -> safe(s.getRuns()); // cricket
            };

            if (keyValue > topScorerValue) {
                topScorerValue = keyValue;
                topScorerStats = s;
            }
        }

        // Write cricket totals
        dto.setTotalRunsScored(totalRuns);
        dto.setTotalWicketsTaken(totalWickets);
        dto.setTotalFours(totalFours);
        dto.setTotalSixes(totalSixes);
        dto.setTotalCatches(totalCatches);
        dto.setHighestTeamScore(highestIndividual);

        // Write multi-sport totals
        dto.setTotalGoals(totalGoals);
        dto.setTotalAssists(totalAssists);
        dto.setTotalFouls(totalFouls);
        dto.setTotalYellowCards(totalYellow);
        dto.setTotalRedCards(totalRed);

        // Top performer
        if (topScorerStats != null && topScorerStats.getPlayer() != null) {
            dto.setTopScorerPlayerId(topScorerStats.getPlayer().getId());
            dto.setTopScorerName(topScorerStats.getPlayer().getName());
            dto.setTopScorerStat(buildTopScorerLabel(sport, topScorerValue));
        }
    }

    private String buildTopScorerLabel(String sport, int value) {
        return switch (sport) {
            case "futsal"                                        -> value + " goals";
            case "volleyball"                                    -> value + " points";
            case "badminton", "table tennis", "tabletennis"     -> value + " points";
            case "ludo"                                          -> value + " home runs";
            case "chess"                                         -> value + " wins";
            case "tug_of_war", "tug of war"                     -> value + " rounds won";
            default                                              -> value + " runs";
        };
    }

    private int safe(Integer v) {
        return v == null ? 0 : v;
    }

    @CacheEvict(value = {"teamByTournamentId", "teams", "teamById", "teamByTournamentIdAndAccountId", "teamByPlayers"}, allEntries = true)
    @Transactional
    public ResponseEntity<?> reuseTeam(Long sourceTeamId, Long targetTournamentId, Long creatorPlayerId) {
        Team source = teamInterface.findById(sourceTeamId).orElse(null);
        Tournament tournament = tournamentInterface.findById(targetTournamentId).orElse(null);
        Player creator = playerInterface.findActiveById(creatorPlayerId).orElse(null);

        if (source == null || tournament == null || creator == null) {
            return ValidationUtils.badRequest("Invalid source team, tournament, or creator");
        }

        // Creator already in this tournament?
        if (playerRequestInterface.findExistingRequest(creatorPlayerId, targetTournamentId) != null) {
            return ValidationUtils.badRequest("You already belong to a team in this tournament");
        }

        // Create new team
        Team newTeam = new Team();
        newTeam.setName(source.getName());
        newTeam.setTournament(tournament);
        newTeam.setCreator(creator);
        newTeam.setStatus("DRAFT");
        Team saved = teamInterface.save(newTeam);

        // Auto-approve creator
        PlayerRequest creatorPR = new PlayerRequest();
        creatorPR.setPlayer(creator);
        creatorPR.setTeam(saved);
        creatorPR.setTournament(tournament);
        creatorPR.setStatus(Constants.STATUS_APPROVED);
        playerRequestInterface.save(creatorPR);

        creator.setTeam(saved);
        creator.setPlayerRole(Constants.ROLE_CAPTAIN);
        playerInterface.save(creator);

        // Invite all old approved players
        List<Player> oldPlayers = playerRequestInterface.findApprovedPlayersByTeamId(sourceTeamId);

        int sent = 0;
        for (Player p : oldPlayers) {
            if (p.getId().equals(creatorPlayerId)) continue;
            // Skip if player already in this tournament
            if (playerRequestInterface.findExistingRequest(p.getId(), targetTournamentId) != null) continue;

            PlayerRequest pr = new PlayerRequest();
            pr.setPlayer(p);
            pr.setTeam(saved);
            pr.setTournament(tournament);
            pr.setStatus("PENDING");
            playerRequestInterface.save(pr);

            // Notify each player
            if (p.getAccount() != null) {
                notificationService.createNotification(
                        p.getAccount(),
                        "Team Invitation 🏆",
                        creator.getName() + " invited you to join " + saved.getName() + " in " + tournament.getName(),
                        NotificationType.FIXTURE
                );
            }
            sent++;
        }

        return ResponseEntity.ok(Map.of(
                "message", "Team reused successfully",
                "newTeamId", saved.getId(),
                "teamName", saved.getName(),
                "invitesSent", sent
        ));
    }

    public ResponseEntity<?> getPlayerTeamHistory(Long playerId) {
        List<PlayerRequest> requests = playerRequestInterface.findAllByPlayer_Id(playerId)
                .stream()
                .filter(pr -> Constants.STATUS_APPROVED.equals(pr.getStatus()))
                .toList();

        List<Map<String, Object>> result = requests.stream().map(pr -> {
            Team t = pr.getTeam();
            List<Player> members = playerRequestInterface.findApprovedPlayersByTeamId(t.getId());

            Map<String, Object> m = new HashMap<>();
            m.put("teamId", t.getId());
            m.put("teamName", t.getName());
            m.put("tournamentId", t.getTournament().getId());
            m.put("tournamentName", t.getTournament().getName());
            m.put("sport", t.getTournament().getSport() != null ? t.getTournament().getSport().getName() : "Unknown");
            m.put("playerCount", members.size());
            m.put("teamStatus", t.getStatus());
            return m;
        }).toList();

        return ResponseEntity.ok(result);
    }
}
