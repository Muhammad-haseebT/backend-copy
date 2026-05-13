package com.livescore.backend.Sport.TugOfWar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.ScoringDTOs.TugOfWarEventDTO;
import com.livescore.backend.DTO.ScoringDTOs.TugOfWarScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.TugOfWar.TugOfWarEvent;
import com.livescore.backend.Entity.TugOfWar.TugOfWarMatchState;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.TeamInterface;
import com.livescore.backend.Interface.multisportgeneric.ScoringServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service("TUGOFWAR")
@RequiredArgsConstructor
public class TugOfWarScoringService implements ScoringServiceInterface {

    private final TugOfWarEventInterface      towEventInterface;
    private final TugOfWarMatchStateInterface towStateInterface;
    private final MatchInterface              matchInterface;
    private final TeamInterface               teamInterface;
    private final TugOfWarStatsService        towStatsService;
    private final TugOfWarPtsTableService     towPtsTableService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Cacheable(value = "towStates", key = "#matchId")
    @Transactional
    public Object getCurrentMatchState(Long matchId) {
        TugOfWarMatchState state = towStateInterface.findByMatch_Id(matchId)
                .orElseGet(() -> createInitialState(matchId));
        return toDTO(state, "");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CachePut(value = "towStates", key = "#result.matchId")
    public Object scoring(JsonNode rawPayload) {
        TugOfWarScoreDTO req = objectMapper.convertValue(rawPayload, TugOfWarScoreDTO.class);
        return process(req);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "towStates", key = "#matchId")
    public Object undoLastBall(Long matchId, Long unused) {
        return undoLast(matchId);
    }

    // ─────────────────────────────────────────
    private TugOfWarScoreDTO process(TugOfWarScoreDTO req) {
        TugOfWarMatchState state = towStateInterface.findByMatch_Id(req.getMatchId())
                .orElseGet(() -> createInitialState(req.getMatchId()));
        Match match = matchInterface.findById(req.getMatchId()).get();

        switch (req.getEventType().toUpperCase()) {
            case "ROUND_WIN" -> handleRoundWin(req, state, match);
            case "END_MATCH" -> handleEndMatch(state, match);
            default -> throw new IllegalArgumentException("Unknown event: " + req.getEventType());
        }

        towStateInterface.save(state);

        if ("COMPLETED".equals(state.getStatus())) {
            towPtsTableService.updateAfterMatch(match.getId());
            towStatsService.onMatchEnd(match.getId());
        }

        return toDTO(state, "");
    }

    // ─────────────────────────────────────────
    // ROUND WIN — main event
    // ─────────────────────────────────────────
    private void handleRoundWin(TugOfWarScoreDTO req, TugOfWarMatchState state, Match match) {
        Team winner = teamInterface.findById(req.getWinnerTeamId()).get();
        boolean t1Won = winner.getId().equals(match.getTeam1().getId());

        if (t1Won) state.setTeam1Rounds(state.getTeam1Rounds() + 1);
        else       state.setTeam2Rounds(state.getTeam2Rounds() + 1);

        // Save event
        TugOfWarEvent ev = buildEvent(match, winner, "ROUND_WIN", state);
        if (state.getRoundStartTime() != null) {
            ev.setRoundDurationSeconds((int) ((System.currentTimeMillis() - state.getRoundStartTime()) / 1000));
        }
        towEventInterface.save(ev);

        // Check match over
        if (state.getTeam1Rounds() >= state.getRoundsToWin()
                || state.getTeam2Rounds() >= state.getRoundsToWin()) {
            state.setStatus("COMPLETED");
            if (state.getTeam1Rounds() > state.getTeam2Rounds()) match.setWinnerTeam(match.getTeam1());
            else                                                   match.setWinnerTeam(match.getTeam2());
            matchInterface.save(match);
        } else {
            // Start next round
            state.setCurrentRound(state.getCurrentRound() + 1);
            state.setRoundStartTime(System.currentTimeMillis());
            state.setStatus("LIVE");
        }
    }

    private void handleEndMatch(TugOfWarMatchState state, Match match) {
        TugOfWarEvent ev = buildEvent(match, null, "END_MATCH", state);
        towEventInterface.save(ev);
        state.setStatus("COMPLETED");
        // Winner = who has more rounds
        if (state.getTeam1Rounds() >= state.getTeam2Rounds()) match.setWinnerTeam(match.getTeam1());
        else                                                    match.setWinnerTeam(match.getTeam2());
        matchInterface.save(match);
    }

    // ─────────────────────────────────────────
    // UNDO
    // ─────────────────────────────────────────
    private TugOfWarScoreDTO undoLast(Long matchId) {
        TugOfWarEvent last = towEventInterface.findTopByMatch_IdOrderByIdDesc(matchId).orElse(null);
        if (last == null) return (TugOfWarScoreDTO) getCurrentMatchState(matchId);

        TugOfWarMatchState state = towStateInterface.findByMatch_Id(matchId).get();

        if ("ROUND_WIN".equals(last.getEventType()) && last.getWinnerTeam() != null) {
            Match match = last.getMatch();
            boolean t1Won = last.getWinnerTeam().getId().equals(match.getTeam1().getId());
            if (t1Won) state.setTeam1Rounds(Math.max(0, state.getTeam1Rounds() - 1));
            else       state.setTeam2Rounds(Math.max(0, state.getTeam2Rounds() - 1));
            if (state.getCurrentRound() > 1) state.setCurrentRound(state.getCurrentRound() - 1);
            state.setStatus("LIVE");
            state.setRoundStartTime(System.currentTimeMillis());
        }

        towEventInterface.delete(last);
        towStateInterface.save(state);
        return toDTO(state, "UNDO");
    }

    // ─────────────────────────────────────────
    // UTIL
    // ─────────────────────────────────────────
    private TugOfWarEvent buildEvent(Match match, Team winner, String type, TugOfWarMatchState state) {
        TugOfWarEvent ev = new TugOfWarEvent();
        ev.setMatch(match); ev.setWinnerTeam(winner);
        ev.setEventType(type); ev.setRoundNumber(state.getCurrentRound());
        ev.setEventTimeSeconds(state.getRoundStartTime() != null
                ? (int) ((System.currentTimeMillis() - state.getRoundStartTime()) / 1000) : 0);
        return ev;
    }

    private TugOfWarMatchState createInitialState(Long matchId) {
        Match match = matchInterface.findById(matchId).get();
        TugOfWarMatchState s = new TugOfWarMatchState();
        s.setMatch(match);
        s.setTeam1Rounds(0); s.setTeam2Rounds(0);
        s.setCurrentRound(1); s.setStatus("LIVE");
        s.setRoundStartTime(System.currentTimeMillis());
        s.setRoundsToWin(match.getSets() != 0 && match.getSets() > 0 ? match.getSets() : 3);
        return towStateInterface.save(s);
    }

    private TugOfWarScoreDTO toDTO(TugOfWarMatchState state, String comment) {
        TugOfWarScoreDTO dto = new TugOfWarScoreDTO();
        dto.setMatchId(state.getMatch().getId());
        dto.setTeam1Rounds(state.getTeam1Rounds());
        dto.setTeam2Rounds(state.getTeam2Rounds());
        dto.setCurrentRound(state.getCurrentRound());
        dto.setRoundsToWin(state.getRoundsToWin());
        dto.setTotalRounds(state.getRoundsToWin() * 2 - 1);
        dto.setStatus(state.getStatus());
        dto.setRoundStartTime(state.getRoundStartTime());
        dto.setComment(comment);

        List<TugOfWarEvent> events = towEventInterface.findByMatch_IdOrderByIdAsc(state.getMatch().getId());
        dto.setTugOfWarEvents(events.stream().map(this::toEventDTO).collect(Collectors.toList()));
        return dto;
    }

    private TugOfWarEventDTO toEventDTO(TugOfWarEvent ev) {
        TugOfWarEventDTO dto = new TugOfWarEventDTO();
        dto.setId(ev.getId()); dto.setEventType(ev.getEventType());
        dto.setRoundNumber(ev.getRoundNumber());
        dto.setEventTimeSeconds(ev.getEventTimeSeconds());
        dto.setRoundDurationSeconds(ev.getRoundDurationSeconds());
        if (ev.getWinnerTeam() != null) {
            dto.setWinnerTeamId(ev.getWinnerTeam().getId());
            dto.setWinnerTeamName(ev.getWinnerTeam().getName());
        }
        return dto;
    }
}
