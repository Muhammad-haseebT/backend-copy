package com.livescore.backend.Sport.Futsal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.PlayerSimpleDTO;
import com.livescore.backend.DTO.ScoringDTOs.FutsalEventDTO;
import com.livescore.backend.DTO.ScoringDTOs.FutsalScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Futsal.FutsalEvent;
import com.livescore.backend.Entity.Futsal.FutsalMatchState;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.PlayerInterface;
import com.livescore.backend.Interface.PlayerRequestInterface;
import com.livescore.backend.Interface.TeamInterface;
import com.livescore.backend.Interface.multisportgeneric.ScoringServiceInterface;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("FUTSAL")
@RequiredArgsConstructor
public class FutsalScoringService implements ScoringServiceInterface {

    private final FutsalEventInterface futsalEventInterface;
    private final FutsalMatchStateInterface futsalMatchStateInterface;
    private final MatchInterface matchInterface;
    private final PlayerInterface playerInterface;
    private final TeamInterface teamInterface;
    private final FutsalStatsService futsalStatsService;
    private final PlayerRequestInterface playerRequestInterface;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────
    // ScoringServiceInterface — 3 methods
    // ─────────────────────────────────────────

    @Override


    @Cacheable(value = "futsalStates", key = "#matchId")
    @Transactional
    public Object getCurrentMatchState(Long matchId) {
        FutsalMatchState state = futsalMatchStateInterface
                .findByMatch_Id(matchId)
                .orElseGet(() -> createInitialState(matchId));
        return toDTO(state, "");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CachePut(value = "futsalStates", key = "#result.matchId")
    public Object scoring(JsonNode rawPayload) {
        FutsalScoreDTO score = objectMapper.convertValue(rawPayload, FutsalScoreDTO.class);
        return scoreFutsal(score);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "futsalStates", key = "#matchId")
    public Object undoLastBall(Long matchId, Long inningsId) {
        return undoLastEvent(matchId);
    }

    // ─────────────────────────────────────────
    // SCORING
    // ─────────────────────────────────────────

    private FutsalScoreDTO scoreFutsal(FutsalScoreDTO score) {
        FutsalMatchState state = futsalMatchStateInterface
                .findByMatch_Id(score.getMatchId())
                .orElseGet(() -> createInitialState(score.getMatchId()));
        Match match = matchInterface.findById(score.getMatchId()).get();

        switch (score.getEventType().toUpperCase()) {
            case "GOAL":
                handleGoal(score, state, match, false);
                break;
            case "OWN_GOAL":
                score.setGoalType("OWN_GOAL");
                handleGoal(score, state, match, true);
                break;
            case "FOUL":
                handleFoul(score, state, match);
                break;
            case "YELLOW_CARD":
                score.setCardType("YELLOW");
                handleFoul(score, state, match);
                break;
            case "RED_CARD":
                score.setCardType("RED");
                handleFoul(score, state, match);
                break;
            case "SUBSTITUTION":
                handleSubstitution(score, state, match);
                break;
            case "END_HALF":
                handleEndHalf(state, match);
                break;
            case "EXTRA_TIME":
                handleExtraTime(state, match);
                break;
            case "TIMEOUT":
                handleTimeout(score, state, match);
                break;
            case "START_SECOND_HALF":
                handleStartSecondHalf(state);
                break;
            default:
                throw new IllegalArgumentException("Unknown eventType: " + score.getEventType());
        }


        futsalMatchStateInterface.save(state);

        return toDTO(state, "");
    }

    private void handleStartSecondHalf(FutsalMatchState state) {
        state.setStatus("LIVE");
        state.setHalfStartTime(System.currentTimeMillis());
    }

    // ─────────────────────────────────────────
    // UNDO
    // ─────────────────────────────────────────

    private FutsalScoreDTO undoLastEvent(Long matchId) {
        FutsalEvent last = futsalEventInterface
                .findTopByMatch_IdOrderByIdDesc(matchId).orElse(null);

        if (last == null) return (FutsalScoreDTO) getCurrentMatchState(matchId);

        FutsalMatchState state = futsalMatchStateInterface.findByMatch_Id(matchId).get();

        switch (last.getEventType().toUpperCase()) {
            case "GOAL":
            case "OWN_GOAL":
                undoGoal(last, state);
                break;
            case "FOUL":
            case "YELLOW_CARD":
            case "RED_CARD":
                undoFoul(last, state);
                break;
            case "END_HALF":
                undoEndHalf(last, state);
                break;
            case "EXTRA_TIME":
                undoExtraTime(state);
                break;
            case "SUBSTITUTION":
                undoSubstitution(last, state, last.getMatch());
                break;
            // SUBSTITUTION, TIMEOUT: just delete event, no state rollback
        }

        futsalEventInterface.delete(last);
        futsalMatchStateInterface.save(state);
        return toDTO(state, "UNDO");
    }

    // ─────────────────────────────────────────
    // HANDLERS
    // ─────────────────────────────────────────
    private void undoSubstitution(FutsalEvent last, FutsalMatchState state, Match match) {
        if (last.getPlayer() == null || last.getInPlayer() == null) return;
        Long outId = last.getPlayer().getId();    // was removed from field
        Long inId  = last.getInPlayer().getId();  // was added to field
        boolean isTeam1 = last.getTeam().getId().equals(match.getTeam1().getId());
        // Reverse the swap: remove inPlayer, restore outPlayer
        if (isTeam1) state.setTeam1OnFieldIds(swapPlayer(state.getTeam1OnFieldIds(), inId, outId));
        else         state.setTeam2OnFieldIds(swapPlayer(state.getTeam2OnFieldIds(), inId, outId));
    }
    private void handleGoal(FutsalScoreDTO score, FutsalMatchState state, Match match, boolean forceOwnGoal) {
        Player player = playerInterface.findActiveById(score.getPlayerId()).get();
        Team   team   = teamInterface.findById(score.getTeamId()).get();

        boolean ownGoal = forceOwnGoal || "OWN_GOAL".equalsIgnoreCase(score.getGoalType());
        boolean isTeam1 = team.getId().equals(match.getTeam1().getId());

        // Own goal: opponent gets the point
        if (ownGoal) {
            if (isTeam1) state.setTeam2Score(state.getTeam2Score() + 1);
            else         state.setTeam1Score(state.getTeam1Score() + 1);
        } else {
            if (isTeam1) state.setTeam1Score(state.getTeam1Score() + 1);
            else         state.setTeam2Score(state.getTeam2Score() + 1);
        }

        FutsalEvent ev = buildEvent(match, team, player,
                ownGoal ? "OWN_GOAL" : "GOAL", state);
        ev.setGoalType(score.getGoalType() != null ? score.getGoalType() : "NORMAL");
        ev.setExtraTime(state.getInExtraTime());

        // Assist
        if (score.getAssistPlayerId() != null && !ownGoal) {
            Player assist = playerInterface.findActiveById(score.getAssistPlayerId()).get();
            ev.setAssistPlayer(assist);
        }

        futsalEventInterface.save(ev);
        futsalStatsService.onEventSaved(ev);
    }

    private void handleFoul(FutsalScoreDTO score, FutsalMatchState state, Match match) {
        Player player = playerInterface.findActiveById(score.getPlayerId()).get();
        Team   team   = teamInterface.findById(score.getTeamId()).get();

        boolean isTeam1 = team.getId().equals(match.getTeam1().getId());

        // Always increment fouls for any disciplinary event
        if (isTeam1) state.setTeam1Fouls(state.getTeam1Fouls() + 1);
        else         state.setTeam2Fouls(state.getTeam2Fouls() + 1);

        // Card tracking (cards don't reset between halves)
        String cardType = score.getCardType();
        if ("YELLOW".equalsIgnoreCase(cardType)) {
            if (isTeam1) state.setTeam1YellowCards(state.getTeam1YellowCards() + 1);
            else         state.setTeam2YellowCards(state.getTeam2YellowCards() + 1);
        } else if ("RED".equalsIgnoreCase(cardType)) {
            if (isTeam1) state.setTeam1RedCards(state.getTeam1RedCards() + 1);
            else         state.setTeam2RedCards(state.getTeam2RedCards() + 1);
        }

        // Determine eventType for storage
        String evType = "FOUL";
        if ("YELLOW".equalsIgnoreCase(cardType)) evType = "YELLOW_CARD";
        else if ("RED".equalsIgnoreCase(cardType)) evType = "RED_CARD";

        FutsalEvent ev = buildEvent(match, team, player, evType, state);
        ev.setCardType(cardType);
        ev.setExtraTime(state.getInExtraTime());
        futsalEventInterface.save(ev);
        futsalStatsService.onEventSaved(ev);
    }

    private void handleSubstitution(FutsalScoreDTO score, FutsalMatchState state, Match match) {
        Team team = teamInterface.findById(score.getTeamId()).get();
        Long outId = score.getOutPlayerId() != null ? score.getOutPlayerId() : score.getPlayerId();
        Long inId  = score.getInPlayerId();
        boolean isTeam1 = team.getId().equals(match.getTeam1().getId());

        // ── Update on-field IDs ─────────────────────────────────────
        if (isTeam1) state.setTeam1OnFieldIds(swapPlayer(state.getTeam1OnFieldIds(), outId, inId));
        else         state.setTeam2OnFieldIds(swapPlayer(state.getTeam2OnFieldIds(), outId, inId));

        FutsalEvent ev = buildEvent(match, team, null, "SUBSTITUTION", state);
        if (outId != null) ev.setPlayer(playerInterface.findActiveById(outId).orElse(null));
        if (inId  != null) ev.setInPlayer(playerInterface.findActiveById(inId).orElse(null));
        ev.setExtraTime(state.getInExtraTime());
        futsalEventInterface.save(ev);
    }
    private void handleTimeout(FutsalScoreDTO score, FutsalMatchState state, Match match) {
        Team team = score.getTeamId() != null
                ? teamInterface.findById(score.getTeamId()).get() : null;
        FutsalEvent ev = buildEvent(match, team, null, "TIMEOUT", state);
        ev.setExtraTime(state.getInExtraTime());
        futsalEventInterface.save(ev);
    }

    protected void handleEndHalf(FutsalMatchState state, Match match) {
        int halfJustEnded = state.getCurrentHalf();

        FutsalEvent ev = buildEvent(match, null, null, "END_HALF", state);
        ev.setHalf(halfJustEnded);
        futsalEventInterface.save(ev);

        if (state.getCurrentHalf() == 1) {
            // First half ended → Half Time
            state.setStatus("HALF_TIME");
            state.setCurrentHalf(2);
            // Futsal rule: fouls reset for each half
            state.setTeam1Fouls(0);
            state.setTeam2Fouls(0);
            state.setHalfStartTime(null);
            state.setInExtraTime(false);
        } else if (state.getCurrentHalf() == 2) {
            // Second half ended
            if (state.getTeam1Score().equals(state.getTeam2Score())) {
                // Draw → offer extra time (frontend will ask)
                state.setStatus("EXTRA_TIME");
            } else {
                // Determine winner
                state.setStatus("COMPLETED");
                match.setStatus("COMPLETED");
                matchInterface.save(match);

                determineWinner(state, match);
                futsalStatsService.onMatchEnd(match.getId(),state.getTeam1Score(),state.getTeam2Score());
            }
            state.setHalfStartTime(null);
        } else {
            // Extra time ended
            state.setStatus("COMPLETED");
            determineWinner(state, match);
            state.setHalfStartTime(null);
        }
    }

    private void handleExtraTime(FutsalMatchState state, Match match) {
        // Start extra time
        state.setStatus("LIVE");
        state.setCurrentHalf(3);  // half=3 means extra time
        state.setInExtraTime(true);
        state.setHalfStartTime(System.currentTimeMillis());
        state.setHalfDurationMinutes(5); // extra time = 5 min in futsal
        // Fouls reset for extra time too
        state.setTeam1Fouls(0);
        state.setTeam2Fouls(0);

        FutsalEvent ev = buildEvent(match, null, null, "EXTRA_TIME", state);
        futsalEventInterface.save(ev);
    }

    // ─────────────────────────────────────────
    // UNDO HELPERS
    // ─────────────────────────────────────────

    private void undoGoal(FutsalEvent last, FutsalMatchState state) {
        boolean ownGoal = "OWN_GOAL".equalsIgnoreCase(last.getGoalType());
        boolean isTeam1 = last.getTeam().getId().equals(last.getMatch().getTeam1().getId());

        if (ownGoal) {
            if (isTeam1) state.setTeam2Score(Math.max(0, state.getTeam2Score() - 1));
            else         state.setTeam1Score(Math.max(0, state.getTeam1Score() - 1));
        } else {
            if (isTeam1) state.setTeam1Score(Math.max(0, state.getTeam1Score() - 1));
            else         state.setTeam2Score(Math.max(0, state.getTeam2Score() - 1));
        }
    }

    private void undoFoul(FutsalEvent last, FutsalMatchState state) {
        boolean isTeam1 = last.getTeam().getId().equals(last.getMatch().getTeam1().getId());

        if (isTeam1) state.setTeam1Fouls(Math.max(0, state.getTeam1Fouls() - 1));
        else         state.setTeam2Fouls(Math.max(0, state.getTeam2Fouls() - 1));

        String card = last.getCardType();
        if ("YELLOW".equalsIgnoreCase(card)) {
            if (isTeam1) state.setTeam1YellowCards(Math.max(0, state.getTeam1YellowCards() - 1));
            else         state.setTeam2YellowCards(Math.max(0, state.getTeam2YellowCards() - 1));
        } else if ("RED".equalsIgnoreCase(card)) {
            if (isTeam1) state.setTeam1RedCards(Math.max(0, state.getTeam1RedCards() - 1));
            else         state.setTeam2RedCards(Math.max(0, state.getTeam2RedCards() - 1));
        }
    }

    private void undoEndHalf(FutsalEvent last, FutsalMatchState state) {
        state.setCurrentHalf(last.getHalf());
        state.setStatus("LIVE");
        state.setInExtraTime(false);
    }

    private void undoExtraTime(FutsalMatchState state) {
        state.setCurrentHalf(2);
        state.setStatus("EXTRA_TIME"); // back to showing draw screen
        state.setInExtraTime(false);
        state.setHalfDurationMinutes(state.getHalfDurationMinutes());
    }

    // ─────────────────────────────────────────
    // UTILITY
    // ─────────────────────────────────────────

    private FutsalEvent buildEvent(Match match, Team team, Player player,
                                   String type, FutsalMatchState state) {
        FutsalEvent ev = new FutsalEvent();
        ev.setMatch(match);
        ev.setTeam(team);
        ev.setPlayer(player);
        ev.setEventType(type);
        ev.setHalf(state.getCurrentHalf());
        ev.setEventTimeSeconds(calcElapsedSeconds(state));
        return ev;
    }

    private int calcElapsedSeconds(FutsalMatchState state) {
        if (state.getHalfStartTime() == null) return 0;
        return (int) ((System.currentTimeMillis() - state.getHalfStartTime()) / 1000);
    }

    private void determineWinner(FutsalMatchState state, Match match) {
        if (state.getTeam1Score() > state.getTeam2Score())
            match.setWinnerTeam(match.getTeam1());
        else if (state.getTeam2Score() > state.getTeam1Score())
            match.setWinnerTeam(match.getTeam2());
        // draw: winnerTeam stays null

        match.setStatus("COMPLETED");
        matchInterface.save(match);
    }

    private FutsalMatchState createInitialState(Long matchId) {
        Match match = matchInterface.findById(matchId).get();
        FutsalMatchState s = new FutsalMatchState();
        s.setMatch(match);
        s.setTeam1Score(0);   s.setTeam2Score(0);
        s.setTeam1Fouls(0);   s.setTeam2Fouls(0);
        s.setTeam1YellowCards(0); s.setTeam2YellowCards(0);
        s.setTeam1RedCards(0);    s.setTeam2RedCards(0);
        s.setCurrentHalf(1);
        s.setStatus("LIVE");
        s.setInExtraTime(false);
        s.setHalfStartTime(System.currentTimeMillis());
        s.setHalfDurationMinutes(match.getHalfDurationMins() != null ? match.getHalfDurationMins() : 25);
        // ── Lineup from match entity ────────────────────────────────
        if (match.getTeam1PlayingIds() != null && !match.getTeam1PlayingIds().isBlank())
            s.setTeam1OnFieldIds(match.getTeam1PlayingIds());
        if (match.getTeam2PlayingIds() != null && !match.getTeam2PlayingIds().isBlank())
            s.setTeam2OnFieldIds(match.getTeam2PlayingIds());
        // ───────────────────────────────────────────────────────────
        return futsalMatchStateInterface.save(s);
    }
    // ─────────────────────────────────────────
    // DTO CONVERTERS
    // ─────────────────────────────────────────

    private FutsalScoreDTO toDTO(FutsalMatchState state, String comment) {
        FutsalScoreDTO dto = new FutsalScoreDTO();
        dto.setMatchId(state.getMatch().getId());
        dto.setTeam1Score(state.getTeam1Score());
        dto.setTeam2Score(state.getTeam2Score());
        dto.setTeam1Fouls(state.getTeam1Fouls());
        dto.setTeam2Fouls(state.getTeam2Fouls());
        dto.setTeam1YellowCards(state.getTeam1YellowCards());
        dto.setTeam2YellowCards(state.getTeam2YellowCards());
        dto.setTeam1RedCards(state.getTeam1RedCards());
        dto.setTeam2RedCards(state.getTeam2RedCards());
        dto.setCurrentHalf(state.getCurrentHalf());
        dto.setStatus(state.getStatus());
        dto.setInExtraTime(state.getInExtraTime() != null && state.getInExtraTime());
        dto.setHalfStartTime(state.getHalfStartTime());
        dto.setHalfDurationMinutes(state.getHalfDurationMinutes() != null ? state.getHalfDurationMinutes() : 25);
        dto.setComment(comment);

        List<FutsalEvent> events = futsalEventInterface
                .findByMatch_IdOrderByIdAsc(state.getMatch().getId());
        dto.setFutsalEvents(events.stream().map(this::eventToDTO).collect(Collectors.toList()));
        Match match = state.getMatch();
        List<Player> squad1 = playerRequestInterface.findApprovedPlayersByTeamId(match.getTeam1().getId());
        List<Player> squad2 = playerRequestInterface.findApprovedPlayersByTeamId(match.getTeam2().getId());
        dto.setTeam1Players(toSimpleDTOs(squad1));
        dto.setTeam2Players(toSimpleDTOs(squad2));
        dto.setTeam1OnField(resolveOnField(squad1, state.getTeam1OnFieldIds()));
        dto.setTeam2OnField(resolveOnField(squad2, state.getTeam2OnFieldIds()));
        return dto;
    }

    private FutsalEventDTO eventToDTO(FutsalEvent ev) {
        FutsalEventDTO dto = new FutsalEventDTO();
        dto.setId(ev.getId());
        dto.setEventType(ev.getEventType());
        dto.setGoalType(ev.getGoalType());
        dto.setCardType(ev.getCardType());
        dto.setHalf(ev.getHalf());
        dto.setEventTimeSeconds(ev.getEventTimeSeconds());
        dto.setExtraTime(ev.getExtraTime());

        if (ev.getPlayer() != null) {
            dto.setScorerId(ev.getPlayer().getId());
            dto.setScorerName(ev.getPlayer().getName());
        }
        if (ev.getAssistPlayer() != null) {
            dto.setAssistPlayerId(ev.getAssistPlayer().getId());
            dto.setAssistPlayerName(ev.getAssistPlayer().getName());
        }
        if (ev.getTeam() != null) {
            dto.setTeamId(ev.getTeam().getId());
            dto.setTeamName(ev.getTeam().getName());
        }
        if (ev.getInPlayer() != null) {
            dto.setInPlayerId(ev.getInPlayer().getId());
            dto.setInPlayerName(ev.getInPlayer().getName());
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