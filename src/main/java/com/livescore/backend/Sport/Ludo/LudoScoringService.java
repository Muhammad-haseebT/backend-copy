package com.livescore.backend.Sport.Ludo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.ScoringDTOs.LudoEventDTO;
import com.livescore.backend.DTO.ScoringDTOs.LudoScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Ludo.LudoEvent;
import com.livescore.backend.Entity.Ludo.LudoMatchState;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.TeamInterface;
import com.livescore.backend.Interface.multisportgeneric.ScoringServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service("LUDO")
@RequiredArgsConstructor
public class LudoScoringService implements ScoringServiceInterface {

    private final LudoEventInterface      ludoEventInterface;
    private final LudoMatchStateInterface ludoStateInterface;
    private final MatchInterface          matchInterface;
    private final PlayerInterface         playerInterface;
    private final TeamInterface           teamInterface;
    private final LudoStatsService        ludoStatsService;
    private final LudoPtsTableService     ludoPtsTableService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Cacheable(value = "ludoStates", key = "#matchId")
    @Transactional  // FIX: was missing — caused LazyInitializationException on winnerTeam
    public Object getCurrentMatchState(Long matchId) {
        LudoMatchState state = ludoStateInterface.findByMatch_Id(matchId)
                .orElseGet(() -> createInitialState(matchId));
        return toDTO(state, "");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CachePut(value = "ludoStates", key = "#result.matchId")
    public Object scoring(JsonNode rawPayload) {
        LudoScoreDTO req = objectMapper.convertValue(rawPayload, LudoScoreDTO.class);
        return process(req);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "ludoStates", key = "#matchId")
    public Object undoLastBall(Long matchId, Long unused) {
        return undoLast(matchId);
    }

    // ─────────────────────────────────────────
    private LudoScoreDTO process(LudoScoreDTO req) {
        LudoMatchState state = ludoStateInterface.findByMatch_Id(req.getMatchId())
                .orElseGet(() -> createInitialState(req.getMatchId()));
        Match match = matchInterface.findById(req.getMatchId()).get();

        if (!"HOME_RUN".equalsIgnoreCase(req.getEventType()))
            throw new IllegalArgumentException("Unknown event: " + req.getEventType());

        LudoEvent saved = handleHomeRun(req, state, match);

        ludoStateInterface.save(state);
        ludoStatsService.onEventSaved(saved);
        if ("COMPLETED".equals(state.getStatus())) {
            ludoPtsTableService.updateAfterMatch(match.getId());
            ludoStatsService.onMatchEnd(match.getId());
        }
        return toDTO(state, "");
    }

    // ── HOME_RUN: auto-win when maxHomeRuns reached ───────────────
    private LudoEvent handleHomeRun(LudoScoreDTO req, LudoMatchState state, Match match) {
        // Frontend sends maxHomeRuns on every event as safety net
        if (req.getMaxHomeRuns() != null && req.getMaxHomeRuns() > 0)
            state.setMaxHomeRuns(req.getMaxHomeRuns());
        int max = state.getMaxHomeRuns() != null ? state.getMaxHomeRuns() : 4;

        Team team = teamInterface.findById(req.getTeamId()).get();
        Player plyr = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;
        boolean t1 = team.getId().equals(match.getTeam1().getId());

        if (t1 && state.getTeam1HomeRuns() >= max)
            throw new IllegalStateException("Team 1 already has all pieces home");
        if (!t1 && state.getTeam2HomeRuns() >= max)
            throw new IllegalStateException("Team 2 already has all pieces home");

        if (t1) state.setTeam1HomeRuns(state.getTeam1HomeRuns() + 1);
        else    state.setTeam2HomeRuns(state.getTeam2HomeRuns() + 1);

        LudoEvent ev = ludoEventInterface.save(build(match, team, plyr, "HOME_RUN", state));

        if ((t1 && state.getTeam1HomeRuns() >= max) || (!t1 && state.getTeam2HomeRuns() >= max)) {
            state.setStatus("COMPLETED");
            match.setWinnerTeam(team);
            matchInterface.save(match);
        }
        return ev;
    }

    // ── UNDO ─────────────────────────────────────────────────────
    private LudoScoreDTO undoLast(Long matchId) {
        LudoEvent last = ludoEventInterface.findTopByMatch_IdOrderByIdDesc(matchId).orElse(null);
        if (last == null) return (LudoScoreDTO) getCurrentMatchState(matchId);

        LudoMatchState state = ludoStateInterface.findByMatch_Id(matchId).get();
        Match match = last.getMatch();
        int max = state.getMaxHomeRuns() != null ? state.getMaxHomeRuns() : 4;

        if ("HOME_RUN".equalsIgnoreCase(last.getEventType())) {
            boolean t1 = last.getTeam() != null
                    && last.getTeam().getId().equals(match.getTeam1().getId());
            if (t1) state.setTeam1HomeRuns(Math.max(0, state.getTeam1HomeRuns() - 1));
            else    state.setTeam2HomeRuns(Math.max(0, state.getTeam2HomeRuns() - 1));
            if ("COMPLETED".equals(state.getStatus())) {
                state.setStatus("LIVE");
                match.setWinnerTeam(null);
                matchInterface.save(match);
            }
        }

        ludoEventInterface.delete(last);
        ludoStateInterface.save(state);
        return toDTO(state, "UNDO");
    }

    // ── UTIL ─────────────────────────────────────────────────────

    // FIX: reads matchFormat from DB to set correct maxHomeRuns from the start
    // "2v2" → 8 home runs, default "1v1" → 4 home runs
    private LudoMatchState createInitialState(Long matchId) {
        Match match = matchInterface.findById(matchId).get();
        LudoMatchState s = new LudoMatchState();
        s.setMatch(match);
        s.setTeam1HomeRuns(0);
        s.setTeam2HomeRuns(0);
        // FIX: derive maxHomeRuns from saved matchFormat instead of always defaulting to 4
        int max = "2v2".equalsIgnoreCase(match.getMatchFormat()) ? 8 : 4;
        s.setMaxHomeRuns(max);
        s.setStatus("LIVE");
        s.setMatchStartTime(System.currentTimeMillis());
        return ludoStateInterface.save(s);
    }

    private LudoEvent build(Match match, Team team, Player player, String type, LudoMatchState state) {
        LudoEvent ev = new LudoEvent();
        ev.setMatch(match); ev.setTeam(team); ev.setPlayer(player); ev.setEventType(type);
        ev.setEventTimeSeconds(state.getMatchStartTime() != null
                ? (int) ((System.currentTimeMillis() - state.getMatchStartTime()) / 1000) : 0);
        return ev;
    }

    private LudoScoreDTO toDTO(LudoMatchState state, String comment) {
        // FIX: re-fetch match to avoid LazyInitializationException on winnerTeam
        // (extra query but safe — only called once per WebSocket message)
        Match match = matchInterface.findById(state.getMatch().getId()).get();

        LudoScoreDTO dto = new LudoScoreDTO();
        dto.setMatchId(match.getId());
        dto.setTeam1HomeRuns(state.getTeam1HomeRuns());
        dto.setTeam2HomeRuns(state.getTeam2HomeRuns());
        dto.setMaxHomeRuns(state.getMaxHomeRuns());
        dto.setStatus(state.getStatus());
        dto.setMatchStartTime(state.getMatchStartTime());
        dto.setComment(comment);

        // FIX: set winnerTeamId so frontend shows correct winner/loser banner
        if (match.getWinnerTeam() != null) {
            dto.setWinnerTeamId(match.getWinnerTeam().getId());
        }

        dto.setLudoEvents(ludoEventInterface.findByMatch_IdOrderByIdAsc(match.getId())
                .stream().map(this::toEventDTO).collect(Collectors.toList()));
        return dto;
    }

    private LudoEventDTO toEventDTO(LudoEvent ev) {
        LudoEventDTO dto = new LudoEventDTO();
        dto.setId(ev.getId());
        dto.setEventType(ev.getEventType());
        dto.setEventTimeSeconds(ev.getEventTimeSeconds());
        if (ev.getPlayer() != null) { dto.setPlayerId(ev.getPlayer().getId()); dto.setPlayerName(ev.getPlayer().getName()); }
        if (ev.getTeam()   != null) { dto.setTeamId(ev.getTeam().getId());     dto.setTeamName(ev.getTeam().getName()); }
        return dto;
    }
}