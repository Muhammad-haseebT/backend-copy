package com.livescore.backend.Sport.Volleyball;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.PlayerSimpleDTO;
import com.livescore.backend.DTO.ScoringDTOs.VolleyballEventDTO;
import com.livescore.backend.DTO.ScoringDTOs.VolleyballScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Volleyball.VolleyballEvent;
import com.livescore.backend.Entity.Volleyball.VolleyballMatchState;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.PlayerRequestInterface;
import com.livescore.backend.Interface.TeamInterface;
import com.livescore.backend.Interface.multisportgeneric.ScoringServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
// ADD to imports
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;

@Service("VOLLEYBALL")
@RequiredArgsConstructor
public class VolleyballScoringService implements ScoringServiceInterface {

    private final VolleyballEventInterface volleyballEventInterface;
    private final VolleyballMatchStateInterface volleyballMatchStateInterface;
    private final MatchInterface matchInterface;
    private final PlayerInterface playerInterface;
    private final TeamInterface teamInterface;
    private final VolleyballStatsService volleyballStatsService;
    private final VolleyballPtsTableService volleyballPtsTableService;
    private final PlayerRequestInterface playerRequestInterface;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // ADD field alongside other finals
    private final CacheManager cacheManager;
    private static final int WIN_BY = 2;
    private static final int MAX_TIMEOUTS = 2;

    // ─────────────────────────────────────────
    // ScoringServiceInterface
    // ─────────────────────────────────────────

    @Override
    @Cacheable(value = "vbStates", key = "#matchId")
    @Transactional
    public Object getCurrentMatchState(Long matchId) {
        VolleyballMatchState state = volleyballMatchStateInterface.findByMatch_Id(matchId)
                .orElseGet(() -> createInitialState(matchId));
        return toDTO(state, "");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object scoring(JsonNode rawPayload) {
        VolleyballScoreDTO req = objectMapper.convertValue(rawPayload, VolleyballScoreDTO.class);
        VolleyballScoreDTO result = process(req);
        evictState(result.getMatchId());
        return result;
}
    // WITH
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object undoLastBall(Long matchId, Long unused) {
        VolleyballScoreDTO result = undoLast(matchId);
        evictState(matchId);
        return result;
    }

    private void evictState(Long matchId) {
        var cache = cacheManager.getCache("vbStates");
        if (cache != null) cache.evict(matchId);
    }

    // ─────────────────────────────────────────
    // PROCESS
    // ─────────────────────────────────────────

    private VolleyballScoreDTO process(VolleyballScoreDTO req) {
        VolleyballMatchState state = volleyballMatchStateInterface.findByMatch_Id(req.getMatchId())
                .orElseGet(() -> createInitialState(req.getMatchId()));
        Match match = matchInterface.findById(req.getMatchId()).get();

        VolleyballEvent saved = null;

        switch (req.getEventType().toUpperCase()) {
            case "POINT" -> saved = handlePoint(req, state, match, "POINT");
            case "ACE" -> saved = handlePoint(req, state, match, "ACE");
            case "BLOCK" -> saved = handlePoint(req, state, match, "BLOCK");
            case "ATTACK_ERROR" -> saved = handleError(req, state, match, "ATTACK_ERROR");
            case "SERVICE_ERROR" -> saved = handleError(req, state, match, "SERVICE_ERROR");
            case "SUBSTITUTION" -> saved = handleSub(req, state, match);
            case "TIMEOUT" -> saved = handleTimeout(req, state, match);
            case "END_SET" -> handleEndSet(state, match);
            case "UNDO" -> { return undoLast(req.getMatchId()); }
            default -> throw new IllegalArgumentException("Unknown event: " + req.getEventType());
        }

        volleyballMatchStateInterface.save(state);

        if (saved != null) volleyballStatsService.onEventSaved(saved);
        if ("COMPLETED".equals(state.getStatus())) {
            volleyballPtsTableService.updateAfterMatch(match.getId(),
                    state.getTeam1Sets(), state.getTeam2Sets());
            volleyballStatsService.onMatchEnd(match.getId());
        }

        return toDTO(state, "");
    }

    // ─────────────────────────────────────────
    // UNDO
    // ─────────────────────────────────────────

    private VolleyballScoreDTO undoLast(Long matchId) {
        VolleyballEvent last = volleyballEventInterface
                .findTopByMatch_IdOrderByIdDesc(matchId).orElse(null);
        if (last == null) return (VolleyballScoreDTO) getCurrentMatchState(matchId);

        VolleyballMatchState state = volleyballMatchStateInterface.findByMatch_Id(matchId).get();

        switch (last.getEventType().toUpperCase()) {
            case "POINT", "ACE", "BLOCK" -> undoPoint(last, state, false);
            case "ATTACK_ERROR", "SERVICE_ERROR" -> undoPoint(last, state, true); // opponent had scored
            case "TIMEOUT" -> undoTimeout(last, state);
            case "END_SET" -> undoEndSet(state);
            case "SUBSTITUTION" -> undoSub(last, state, last.getMatch());
        }

        volleyballEventInterface.delete(last);
        volleyballMatchStateInterface.save(state);

        if (last.getPlayer() != null && last.getMatch().getTournament() != null)
            volleyballStatsService.recalculatePlayerStats(
                    last.getPlayer().getId(), last.getMatch().getTournament().getId());

        return toDTO(state, "UNDO");
    }
    private void undoSub(VolleyballEvent last, VolleyballMatchState state, Match match) {
        if (last.getPlayer() == null || last.getInPlayer() == null) return;
        boolean t1 = last.getTeam().getId().equals(match.getTeam1().getId());
        if (t1) state.setTeam1OnFieldIds(swapPlayer(state.getTeam1OnFieldIds(), last.getInPlayer().getId(), last.getPlayer().getId()));
        else    state.setTeam2OnFieldIds(swapPlayer(state.getTeam2OnFieldIds(), last.getInPlayer().getId(), last.getPlayer().getId()));
    }
    // ─────────────────────────────────────────
    // HANDLERS
    // ─────────────────────────────────────────

    /**
     * POINT / ACE / BLOCK — scoring team gets +1
     */
    private VolleyballEvent handlePoint(VolleyballScoreDTO req,
                                        VolleyballMatchState state, Match match, String type) {
        Team team = teamInterface.findById(req.getTeamId()).get();
        Player plyr = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;
        addPoint(team, match, state);
        VolleyballEvent ev = build(match, team, plyr, type, state);
        VolleyballEvent saved = volleyballEventInterface.save(ev);
        checkSetComplete(state, match);
        return saved;
    }

    /**
     * ATTACK_ERROR / SERVICE_ERROR — opponent gets the point
     */
    private VolleyballEvent handleError(VolleyballScoreDTO req,
                                        VolleyballMatchState state, Match match, String type) {
        Team errorTeam = teamInterface.findById(req.getTeamId()).get();
        Player plyr = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;
        Team opponent = errorTeam.getId().equals(match.getTeam1().getId())
                ? match.getTeam2() : match.getTeam1();
        addPoint(opponent, match, state);
        VolleyballEvent ev = build(match, errorTeam, plyr, type, state);
        VolleyballEvent saved = volleyballEventInterface.save(ev);
        checkSetComplete(state, match);
        return saved;
    }

    private VolleyballEvent handleSub(VolleyballScoreDTO req, VolleyballMatchState state, Match match) {
        Team team = teamInterface.findById(req.getTeamId()).get();
        boolean isTeam1 = team.getId().equals(match.getTeam1().getId());
        if (isTeam1)
            state.setTeam1OnFieldIds(swapPlayer(state.getTeam1OnFieldIds(), req.getOutPlayerId(), req.getInPlayerId()));
        else
            state.setTeam2OnFieldIds(swapPlayer(state.getTeam2OnFieldIds(), req.getOutPlayerId(), req.getInPlayerId()));

        VolleyballEvent ev = build(match, team, null, "SUBSTITUTION", state);
        if (req.getOutPlayerId() != null)
            ev.setPlayer(playerInterface.findActiveById(req.getOutPlayerId()).orElse(null));
        if (req.getInPlayerId() != null)
            ev.setInPlayer(playerInterface.findActiveById(req.getInPlayerId()).orElse(null));
        return volleyballEventInterface.save(ev);
    }

    private VolleyballEvent handleTimeout(VolleyballScoreDTO req,
                                          VolleyballMatchState state, Match match) {
        Team team = teamInterface.findById(req.getTeamId()).get();
        boolean t1 = team.getId().equals(match.getTeam1().getId());
        if (t1) state.setTeam1Timeouts(state.getTeam1Timeouts() + 1);
        else state.setTeam2Timeouts(state.getTeam2Timeouts() + 1);
        return volleyballEventInterface.save(build(match, team, null, "TIMEOUT", state));
    }


    private void handleEndSet(VolleyballMatchState state, Match match) {
        VolleyballEvent ev = build(match, null, null, "END_SET", state);
        ev.setSetNumber(state.getCurrentSet());
        volleyballEventInterface.save(ev);

        // Jis team ke zyada points hain — us ko set dedo
        if (state.getTeam1Points() > state.getTeam2Points()) {
            state.setTeam1Sets(state.getTeam1Sets() + 1);
        } else if (state.getTeam2Points() > state.getTeam1Points()) {
            state.setTeam2Sets(state.getTeam2Sets() + 1);
        }
        // Draw mein koi set credit nahi

        // Match over?
        if (state.getTeam1Sets() >= state.getSetsToWin()
                || state.getTeam2Sets() >= state.getSetsToWin()) {
            state.setStatus("COMPLETED");
            match.setStatus("COMPLETED");
            if (state.getTeam1Sets() > state.getTeam2Sets()) match.setWinnerTeam(match.getTeam1());
            else match.setWinnerTeam(match.getTeam2());
            matchInterface.save(match);
        } else {
            startNextSet(state, match);
        }
    }

    // ─────────────────────────────────────────
    // SET WIN DETECTION
    // ─────────────────────────────────────────

    private void checkSetComplete(VolleyballMatchState state, Match match) {
        int totalSets = state.getTeam1Sets() + state.getTeam2Sets();
        int maxSets = state.getSetsToWin() * 2 - 1; // e.g. 5 for best-of-5
        boolean isFinalSet = (totalSets == maxSets - 1); // last possible set

        int required = isFinalSet
                ? safeVal(state.getFinalSetPoints(), 15)
                : safeVal(state.getPointsPerSet(), 25);

        int t1 = state.getTeam1Points();
        int t2 = state.getTeam2Points();

        boolean t1Wins = t1 >= required && (t1 - t2) >= WIN_BY;
        boolean t2Wins = t2 >= required && (t2 - t1) >= WIN_BY;

        if (!t1Wins && !t2Wins) return;

        // Save END_SET marker
        VolleyballEvent ev = build(match, null, null, "END_SET", state);
        ev.setSetNumber(state.getCurrentSet());
        volleyballEventInterface.save(ev);

        if (t1Wins) state.setTeam1Sets(state.getTeam1Sets() + 1);
        else state.setTeam2Sets(state.getTeam2Sets() + 1);

        // Check match over
        if (state.getTeam1Sets() >= state.getSetsToWin()
                || state.getTeam2Sets() >= state.getSetsToWin()) {
            state.setStatus("COMPLETED");
            if (state.getTeam1Sets() > state.getTeam2Sets()) match.setWinnerTeam(match.getTeam1());
            else match.setWinnerTeam(match.getTeam2());
            matchInterface.save(match);
        } else {
            startNextSet(state, match);
        }
    }

    private void startNextSet(VolleyballMatchState state, Match match) {
        state.setCurrentSet(state.getCurrentSet() + 1);
        state.setTeam1Points(0);
        state.setTeam2Points(0);
        state.setTeam1Timeouts(0);
        state.setTeam2Timeouts(0);
        state.setSetStartTime(System.currentTimeMillis());
        state.setStatus("LIVE");
    }

    // ─────────────────────────────────────────
    // UNDO HELPERS
    // ─────────────────────────────────────────

    private void undoPoint(VolleyballEvent last, VolleyballMatchState state, boolean errorEvent) {
        if (last.getTeam() == null) return;
        boolean lastTeamIsTeam1 = last.getTeam().getId().equals(last.getMatch().getTeam1().getId());
        // For errors, opponent got the point — so undo from opponent
        boolean pointWasTeam1 = errorEvent ? !lastTeamIsTeam1 : lastTeamIsTeam1;
        if (pointWasTeam1) state.setTeam1Points(Math.max(0, state.getTeam1Points() - 1));
        else state.setTeam2Points(Math.max(0, state.getTeam2Points() - 1));
    }

    private void undoTimeout(VolleyballEvent last, VolleyballMatchState state) {
        if (last.getTeam() == null) return;
        boolean t1 = last.getTeam().getId().equals(last.getMatch().getTeam1().getId());
        if (t1) state.setTeam1Timeouts(Math.max(0, state.getTeam1Timeouts() - 1));
        else state.setTeam2Timeouts(Math.max(0, state.getTeam2Timeouts() - 1));
    }

    private void undoEndSet(VolleyballMatchState state) {
        if (state.getCurrentSet() > 1) state.setCurrentSet(state.getCurrentSet() - 1);
        state.setTeam1Points(0);
        state.setTeam2Points(0);
        state.setTeam1Timeouts(0);
        state.setTeam2Timeouts(0);
        state.setStatus("LIVE");

        // ❌ Purana — random decrement karta tha
        // if (state.getTeam1Sets() > 0) state.setTeam1Sets(state.getTeam1Sets() - 1);
        // else if (state.getTeam2Sets() > 0) state.setTeam2Sets(state.getTeam2Sets() - 1);

        // ✅ Fix — last END_SET event se pehle jo team aage thi uska set wapas lo
        // Dono mein se jo zyada hai woh haal hi mein jeeta hoga
        if (state.getTeam1Sets() > state.getTeam2Sets())
            state.setTeam1Sets(state.getTeam1Sets() - 1);
        else if (state.getTeam2Sets() > state.getTeam1Sets())
            state.setTeam2Sets(state.getTeam2Sets() - 1);
        // equal mein kuch mat karo
    }
    // ─────────────────────────────────────────
    // UTIL
    // ─────────────────────────────────────────

    private void addPoint(Team team, Match match, VolleyballMatchState state) {
        if (team.getId().equals(match.getTeam1().getId()))
            state.setTeam1Points(state.getTeam1Points() + 1);
        else
            state.setTeam2Points(state.getTeam2Points() + 1);
    }

    private VolleyballEvent build(Match match, Team team, Player player,
                                  String type, VolleyballMatchState state) {
        VolleyballEvent ev = new VolleyballEvent();
        ev.setMatch(match);
        ev.setTeam(team);
        ev.setPlayer(player);
        ev.setEventType(type);
        ev.setSetNumber(state.getCurrentSet());
        ev.setEventTimeSeconds(state.getSetStartTime() != null
                ? (int) ((System.currentTimeMillis() - state.getSetStartTime()) / 1000) : 0);
        return ev;
    }

    private VolleyballMatchState createInitialState(Long matchId) {
        Match match = matchInterface.findById(matchId).get();
        VolleyballMatchState s = new VolleyballMatchState();
        s.setMatch(match);
        s.setTeam1Points(0);
        s.setTeam2Points(0);
        s.setTeam1Sets(0);
        s.setTeam2Sets(0);
        s.setCurrentSet(1);
        s.setStatus("LIVE");
        s.setTeam1Timeouts(0);
        s.setTeam2Timeouts(0);
        s.setSetStartTime(System.currentTimeMillis());
        // ✅ Read config from match
        s.setSetsToWin(match.getSets() > 0 ? match.getSets() : 3);
        s.setPointsPerSet(match.getPointsPerSet() != null ? match.getPointsPerSet() : 25);
        s.setFinalSetPoints(match.getFinalSetPoints() != null ? match.getFinalSetPoints() : 15);
        if (match.getTeam1PlayingIds() != null && !match.getTeam1PlayingIds().isBlank())
            s.setTeam1OnFieldIds(match.getTeam1PlayingIds());
        if (match.getTeam2PlayingIds() != null && !match.getTeam2PlayingIds().isBlank())
            s.setTeam2OnFieldIds(match.getTeam2PlayingIds());
        return volleyballMatchStateInterface.save(s);
    }

    private int safeVal(Integer v, int def) {
        return v != null ? v : def;
    }

    // ─────────────────────────────────────────
    // DTO
    // ─────────────────────────────────────────

    private VolleyballScoreDTO toDTO(VolleyballMatchState state, String comment) {
        VolleyballScoreDTO dto = new VolleyballScoreDTO();
        dto.setMatchId(state.getMatch().getId());
        dto.setTeam1Points(state.getTeam1Points());
        dto.setTeam2Points(state.getTeam2Points());
        dto.setTeam1Sets(state.getTeam1Sets());
        dto.setTeam2Sets(state.getTeam2Sets());
        dto.setCurrentSet(state.getCurrentSet());
        dto.setStatus(state.getStatus());
        dto.setTeam1Timeouts(state.getTeam1Timeouts());
        dto.setTeam2Timeouts(state.getTeam2Timeouts());
        dto.setSetStartTime(state.getSetStartTime());
        dto.setSetsToWin(state.getSetsToWin());
        dto.setPointsPerSet(state.getPointsPerSet());
        dto.setFinalSetPoints(state.getFinalSetPoints());

        // Current set's point target
        int totalSets = state.getTeam1Sets() + state.getTeam2Sets();
        int maxSets = state.getSetsToWin() * 2 - 1;
        boolean isFinal = (totalSets == maxSets - 1);
        dto.setPointsToWin(isFinal
                ? safeVal(state.getFinalSetPoints(), 15)
                : safeVal(state.getPointsPerSet(), 25));

        dto.setComment(comment);
        List<VolleyballEvent> events = volleyballEventInterface
                .findByMatch_IdOrderByIdAscWithPlayers(state.getMatch().getId());
        dto.setVolleyballEvents(events.stream().map(this::toEventDTO).collect(Collectors.toList()));
        Match match = state.getMatch();
        List<Player> squad1 = playerRequestInterface.findApprovedPlayersByTeamId(match.getTeam1().getId());
        List<Player> squad2 = playerRequestInterface.findApprovedPlayersByTeamId(match.getTeam2().getId());
        dto.setTeam1Players(toSimpleDTOs(squad1));
        dto.setTeam2Players(toSimpleDTOs(squad2));
        dto.setTeam1OnField(resolveOnField(squad1, state.getTeam1OnFieldIds()));
        dto.setTeam2OnField(resolveOnField(squad2, state.getTeam2OnFieldIds()));
        return dto;
    }

    private VolleyballEventDTO toEventDTO(VolleyballEvent ev) {
        VolleyballEventDTO dto = new VolleyballEventDTO();
        dto.setId(ev.getId());
        dto.setEventType(ev.getEventType());
        dto.setSetNumber(ev.getSetNumber());
        dto.setEventTimeSeconds(ev.getEventTimeSeconds());
        if (ev.getPlayer() != null) {
            dto.setPlayerId(ev.getPlayer().getId());
            dto.setPlayerName(ev.getPlayer().getName());
        }
        if (ev.getInPlayer() != null) {
            dto.setInPlayerId(ev.getInPlayer().getId());
            dto.setInPlayerName(ev.getInPlayer().getName());
        }
        if (ev.getTeam() != null) {
            dto.setTeamId(ev.getTeam().getId());
            dto.setTeamName(ev.getTeam().getName());
        }
        return dto;
    }


    private Set<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) return new LinkedHashSet<>();
        return Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String swapPlayer(String current, Long outId, Long inId) {
        Set<Long> set = new LinkedHashSet<>(parseIds(current));
        if (outId != null) set.remove(outId);
        if (inId  != null) set.add(inId);
        return set.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<PlayerSimpleDTO> toSimpleDTOs(List<Player> players) {
        return players.stream()
                .map(p -> new PlayerSimpleDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }

    private List<PlayerSimpleDTO> resolveOnField(List<Player> squad, String idsStr) {
        if (idsStr == null || idsStr.isBlank())
            return toSimpleDTOs(squad); // fallback = full squad
        Set<Long> ids = parseIds(idsStr);
        return squad.stream()
                .filter(p -> ids.contains(p.getId()))
                .map(p -> new PlayerSimpleDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }
}