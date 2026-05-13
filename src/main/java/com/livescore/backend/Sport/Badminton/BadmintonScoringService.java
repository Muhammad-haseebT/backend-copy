package com.livescore.backend.Sport.Badminton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.PlayerSimpleDTO;
import com.livescore.backend.DTO.ScoringDTOs.BadmintonEventDTO;
import com.livescore.backend.DTO.ScoringDTOs.BadmintonScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Badminton.BadmintonEvent;
import com.livescore.backend.Entity.Badminton.BadmintonMatchState;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.PlayerRequestInterface;
import com.livescore.backend.Interface.TeamInterface;
import com.livescore.backend.Interface.multisportgeneric.ScoringServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("BADMINTON")
@RequiredArgsConstructor
public class BadmintonScoringService implements ScoringServiceInterface {

    private final BadmintonEventInterface      badmintonEventInterface;
    private final BadmintonMatchStateInterface badmintonMatchStateInterface;
    private final MatchInterface               matchInterface;
    private final PlayerInterface              playerInterface;
    private final TeamInterface                teamInterface;
    private final BadmintonStatsService        badmintonStatsService;
    private final BadmintonPtsTableService     badmintonPtsTableService;
    private final PlayerRequestInterface playerRequestInterface;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int WIN_BY         = 2;  // must lead by 2
    private static final int DEFAULT_POINTS = 21;
    private static final int DEFAULT_MAX    = 30; // 29-29 → next point wins

    // ─────────────────────────────────────────
    @Override
    @Cacheable(value = "badmintonStates", key = "#matchId")
    @Transactional
    public Object getCurrentMatchState(Long matchId) {
        BadmintonMatchState state = badmintonMatchStateInterface.findByMatch_Id(matchId)
                .orElseGet(() -> createInitialState(matchId));
        return toDTO(state, "");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CachePut(value = "badmintonStates", key = "#result.matchId")
    public Object scoring(JsonNode rawPayload) {
        BadmintonScoreDTO req = objectMapper.convertValue(rawPayload, BadmintonScoreDTO.class);
        return process(req);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "badmintonStates", key = "#matchId")
    public Object undoLastBall(Long matchId, Long unused) {
        return undoLast(matchId);
    }

    // ─────────────────────────────────────────
    // PROCESS
    // ─────────────────────────────────────────

    private BadmintonScoreDTO process(BadmintonScoreDTO req) {
        BadmintonMatchState state = badmintonMatchStateInterface.findByMatch_Id(req.getMatchId())
                .orElseGet(() -> createInitialState(req.getMatchId()));
        Match match = matchInterface.findById(req.getMatchId()).get();

        BadmintonEvent saved = null;

        switch (req.getEventType().toUpperCase()) {
            // ── Scoring team gets point ──────────────────────────
            case "POINT":
            case "SMASH":
            case "SERVICE_ACE":
                saved = handleScoringEvent(req, state, match, req.getEventType().toUpperCase());
                break;

            // ── Fault → opponent gets point ──────────────────────
            case "NET_FAULT":
            case "FOOT_FAULT":
            case "OUT":
                saved = handleFaultEvent(req, state, match, req.getEventType().toUpperCase());
                break;

            // ── Non-scoring ───────────────────────────────────────
            case "SUBSTITUTION":
                saved = handleSub(req, state, match);
                break;
            case "END_GAME":
                handleEndGame(state, match);
                break;

            default:
                throw new IllegalArgumentException("Unknown event: " + req.getEventType());
        }

        badmintonMatchStateInterface.save(state);

        if (saved != null) badmintonStatsService.onEventSaved(saved);
        if ("COMPLETED".equals(state.getStatus())) {
            badmintonPtsTableService.updateAfterMatch(match.getId());
            badmintonStatsService.onMatchEnd(match.getId());
        }

        return toDTO(state, "");
    }

    // ─────────────────────────────────────────
    // HANDLERS
    // ─────────────────────────────────────────

    /** POINT / SMASH / SERVICE_ACE — scoring team +1 */
    private BadmintonEvent handleScoringEvent(BadmintonScoreDTO req,
                                               BadmintonMatchState state, Match match, String type) {
        Team team   = teamInterface.findById(req.getTeamId()).get();
        Player plyr = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;
        addPoint(team, match, state);
        BadmintonEvent ev = build(match, team, plyr, type, state);
        BadmintonEvent saved = badmintonEventInterface.save(ev);
        checkGameComplete(state, match);
        return saved;
    }

    /** NET_FAULT / FOOT_FAULT / OUT — fault team's opponent gets +1 */
    private BadmintonEvent handleFaultEvent(BadmintonScoreDTO req,
                                             BadmintonMatchState state, Match match, String type) {
        Team faultTeam = teamInterface.findById(req.getTeamId()).get();
        Player plyr    = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;
        Team opponent  = faultTeam.getId().equals(match.getTeam1().getId())
                ? match.getTeam2() : match.getTeam1();
        addPoint(opponent, match, state);
        BadmintonEvent ev = build(match, faultTeam, plyr, type, state);
        BadmintonEvent saved = badmintonEventInterface.save(ev);
        checkGameComplete(state, match);
        return saved;
    }

    private BadmintonEvent handleSub(BadmintonScoreDTO req,
                                      BadmintonMatchState state, Match match) {
        Team team = teamInterface.findById(req.getTeamId()).get();
        BadmintonEvent ev = build(match, team, null, "SUBSTITUTION", state);
        if (req.getOutPlayerId() != null)
            ev.setPlayer(playerInterface.findActiveById(req.getOutPlayerId()).orElse(null));
        if (req.getInPlayerId() != null)
            ev.setInPlayer(playerInterface.findActiveById(req.getInPlayerId()).orElse(null));
        return badmintonEventInterface.save(ev);
    }

    /** Manual END_GAME — jis ka zyada score, usko game dedo */
    private void handleEndGame(BadmintonMatchState state, Match match) {
        BadmintonEvent ev = build(match, null, null, "END_GAME", state);
        ev.setGameNumber(state.getCurrentGame());
        badmintonEventInterface.save(ev);

        if (state.getTeam1Points() > state.getTeam2Points())
            state.setTeam1Games(state.getTeam1Games() + 1);
        else if (state.getTeam2Points() > state.getTeam1Points())
            state.setTeam2Games(state.getTeam2Games() + 1);

        checkMatchOver(state, match);
    }

    // ─────────────────────────────────────────
    // GAME WIN DETECTION (badminton rules)
    // ─────────────────────────────────────────

    private void checkGameComplete(BadmintonMatchState state, Match match) {
        int pts    = safeVal(state.getPointsPerGame(), DEFAULT_POINTS);
        int maxPts = safeVal(state.getMaxPoints(),     DEFAULT_MAX);
        int t1     = state.getTeam1Points();
        int t2     = state.getTeam2Points();

        boolean t1Wins = (t1 >= pts && (t1 - t2) >= WIN_BY) || t1 >= maxPts;
        boolean t2Wins = (t2 >= pts && (t2 - t1) >= WIN_BY) || t2 >= maxPts;

        if (!t1Wins && !t2Wins) return;

        // Save END_GAME marker
        BadmintonEvent ev = build(match, null, null, "END_GAME", state);
        ev.setGameNumber(state.getCurrentGame());
        ev.setScoreSnapshot(state.getTeam1Points() + "-" + state.getTeam2Points());
        badmintonEventInterface.save(ev);

        if (t1Wins) state.setTeam1Games(state.getTeam1Games() + 1);
        else        state.setTeam2Games(state.getTeam2Games() + 1);

        checkMatchOver(state, match);
    }
    @Caching(evict = {
            @CacheEvict(value = "matches", allEntries = true, beforeInvocation = false),
            @CacheEvict(value = "matchById", allEntries = true, beforeInvocation = false)
    })
    protected void checkMatchOver(BadmintonMatchState state, Match match) {
        if (state.getTeam1Games() >= state.getGamesToWin()
                || state.getTeam2Games() >= state.getGamesToWin()) {
            state.setStatus("COMPLETED");
            match.setStatus("COMPLETED");
            if (state.getTeam1Games() > state.getTeam2Games()) match.setWinnerTeam(match.getTeam1());
            else                                                match.setWinnerTeam(match.getTeam2());
            matchInterface.save(match);
        } else {
            startNextGame(state);
        }
    }

    private void startNextGame(BadmintonMatchState state) {
        state.setCurrentGame(state.getCurrentGame() + 1);
        state.setTeam1Points(0);
        state.setTeam2Points(0);
        state.setGameStartTime(System.currentTimeMillis());
        state.setStatus("LIVE");
    }

    // ─────────────────────────────────────────
    // UNDO
    // ─────────────────────────────────────────

    private BadmintonScoreDTO undoLast(Long matchId) {
        BadmintonEvent last = badmintonEventInterface
                .findTopByMatch_IdOrderByIdDesc(matchId).orElse(null);
        if (last == null) return (BadmintonScoreDTO) getCurrentMatchState(matchId);

        BadmintonMatchState state = badmintonMatchStateInterface.findByMatch_Id(matchId).get();

        switch (last.getEventType().toUpperCase()) {
            case "POINT", "SMASH", "SERVICE_ACE" -> undoPoint(last, state, false);
            case "NET_FAULT", "FOOT_FAULT", "OUT" -> undoPoint(last, state, true); // opponent had scored
            case "END_GAME"                        -> undoEndGame(state);
        }

        badmintonEventInterface.delete(last);
        badmintonMatchStateInterface.save(state);

        if (last.getPlayer() != null && last.getMatch().getTournament() != null)
            badmintonStatsService.recalculatePlayerStats(
                    last.getPlayer().getId(), last.getMatch().getTournament().getId());

        return toDTO(state, "UNDO");
    }

    private void undoPoint(BadmintonEvent last, BadmintonMatchState state, boolean faultEvent) {
        if (last.getTeam() == null) return;
        boolean lastTeamIsT1 = last.getTeam().getId().equals(last.getMatch().getTeam1().getId());
        boolean pointWasT1   = faultEvent ? !lastTeamIsT1 : lastTeamIsT1;
        if (pointWasT1) state.setTeam1Points(Math.max(0, state.getTeam1Points() - 1));
        else            state.setTeam2Points(Math.max(0, state.getTeam2Points() - 1));
    }

    private void undoEndGame(BadmintonMatchState state) {
        if (state.getCurrentGame() > 1) state.setCurrentGame(state.getCurrentGame() - 1);
        state.setTeam1Points(0); state.setTeam2Points(0);
        state.setStatus("LIVE");
        if (state.getTeam1Games() > 0) state.setTeam1Games(state.getTeam1Games() - 1);
        else if (state.getTeam2Games() > 0) state.setTeam2Games(state.getTeam2Games() - 1);
    }

    // ─────────────────────────────────────────
    // UTIL
    // ─────────────────────────────────────────

    private void addPoint(Team team, Match match, BadmintonMatchState state) {
        if (team.getId().equals(match.getTeam1().getId()))
            state.setTeam1Points(state.getTeam1Points() + 1);
        else
            state.setTeam2Points(state.getTeam2Points() + 1);
    }

    private BadmintonEvent build(Match match, Team team, Player player,
                                  String type, BadmintonMatchState state) {
        BadmintonEvent ev = new BadmintonEvent();
        ev.setMatch(match); ev.setTeam(team); ev.setPlayer(player);
        ev.setEventType(type);
        ev.setGameNumber(state.getCurrentGame());
        ev.setEventTimeSeconds(state.getGameStartTime() != null
                ? (int) ((System.currentTimeMillis() - state.getGameStartTime()) / 1000) : 0);
        ev.setScoreSnapshot(state.getTeam1Points() + "-" + state.getTeam2Points());
        return ev;
    }

    private BadmintonMatchState createInitialState(Long matchId) {
        Match match = matchInterface.findById(matchId).get();
        BadmintonMatchState s = new BadmintonMatchState();
        s.setMatch(match);
        s.setTeam1Points(0); s.setTeam2Points(0);
        s.setTeam1Games(0);  s.setTeam2Games(0);
        s.setCurrentGame(1); s.setStatus("LIVE");
        s.setGameStartTime(System.currentTimeMillis());
        // ✅ Config from match (same as volleyball — sets field = games to win)
        s.setGamesToWin(match.getSets() != 0 && match.getSets() > 0 ? match.getSets() : 2);
        s.setPointsPerGame(match.getPointsPerSet() != null ? match.getPointsPerSet() : 21);
        s.setMaxPoints(match.getFinalSetPoints() != null ? match.getFinalSetPoints() : 30);
        if (match.getTeam1PlayingIds() != null && !match.getTeam1PlayingIds().isBlank())
            s.setTeam1PlayerIds(match.getTeam1PlayingIds());
        if (match.getTeam2PlayingIds() != null && !match.getTeam2PlayingIds().isBlank())
            s.setTeam2PlayerIds(match.getTeam2PlayingIds());
        return badmintonMatchStateInterface.save(s);
    }

    private int safeVal(Integer v, int def) { return v != null ? v : def; }

    // ─────────────────────────────────────────
    // DTO
    // ─────────────────────────────────────────

    private BadmintonScoreDTO toDTO(BadmintonMatchState state, String comment) {
        BadmintonScoreDTO dto = new BadmintonScoreDTO();
        dto.setMatchId(state.getMatch().getId());
        dto.setTeam1Points(state.getTeam1Points());
        dto.setTeam2Points(state.getTeam2Points());
        dto.setTeam1Games(state.getTeam1Games());
        dto.setTeam2Games(state.getTeam2Games());
        dto.setCurrentGame(state.getCurrentGame());
        dto.setStatus(state.getStatus());
        dto.setGamesToWin(state.getGamesToWin());
        dto.setPointsPerGame(state.getPointsPerGame());
        dto.setMaxPoints(state.getMaxPoints());
        dto.setGameStartTime(state.getGameStartTime());

        // pointsToWin = 21 normally; at deuce (20-20) extend to lead by 2, cap at maxPoints
        int pts    = safeVal(state.getPointsPerGame(), DEFAULT_POINTS);
        int maxPts = safeVal(state.getMaxPoints(), DEFAULT_MAX);
        int higher = Math.max(state.getTeam1Points(), state.getTeam2Points());
        int toWin  = higher >= pts - 1 ? Math.min(higher + WIN_BY, maxPts) : pts;
        dto.setPointsToWin(toWin);

        dto.setComment(comment);

        List<BadmintonEvent> events = badmintonEventInterface
                .findByMatch_IdOrderByIdAsc(state.getMatch().getId());
        dto.setBadmintonEvents(events.stream().map(this::toEventDTO).collect(Collectors.toList()));
        Match m = state.getMatch();
        List<Player> all1 = playerRequestInterface.findApprovedPlayersByTeamId(m.getTeam1().getId());
        List<Player> all2 = playerRequestInterface.findApprovedPlayersByTeamId(m.getTeam2().getId());
        dto.setTeam1Players(resolvePlayingPlayers(all1, state.getTeam1PlayerIds()));
        dto.setTeam2Players(resolvePlayingPlayers(all2, state.getTeam2PlayerIds()));
        return dto;
    }

    private BadmintonEventDTO toEventDTO(BadmintonEvent ev) {
        BadmintonEventDTO dto = new BadmintonEventDTO();
        dto.setId(ev.getId()); dto.setEventType(ev.getEventType());
        dto.setGameNumber(ev.getGameNumber());
        dto.setEventTimeSeconds(ev.getEventTimeSeconds());
        dto.setScoreSnapshot(ev.getScoreSnapshot());
        if (ev.getPlayer()   != null) { dto.setPlayerId(ev.getPlayer().getId());     dto.setPlayerName(ev.getPlayer().getName()); }
        if (ev.getInPlayer() != null) { dto.setInPlayerId(ev.getInPlayer().getId()); dto.setInPlayerName(ev.getInPlayer().getName()); }
        if (ev.getTeam()     != null) { dto.setTeamId(ev.getTeam().getId());         dto.setTeamName(ev.getTeam().getName()); }
        return dto;
    }
    private Set<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) return new LinkedHashSet<>();
        return Arrays.stream(ids.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).map(Long::parseLong)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    private List<PlayerSimpleDTO> resolvePlayingPlayers(List<Player> squad, String idsStr) {
        if (idsStr == null || idsStr.isBlank()) return squad.stream()
                .map(p -> new PlayerSimpleDTO(p.getId(), p.getName())).collect(Collectors.toList());
        Set<Long> ids = parseIds(idsStr);
        return squad.stream().filter(p -> ids.contains(p.getId()))
                .map(p -> new PlayerSimpleDTO(p.getId(), p.getName())).collect(Collectors.toList());
    }
}
