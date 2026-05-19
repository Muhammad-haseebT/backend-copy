package com.livescore.backend.Sport.Hockey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.PlayerSimpleDTO;
import com.livescore.backend.DTO.ScoringDTOs.HockeyEventDTO;
import com.livescore.backend.DTO.ScoringDTOs.HockeyScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.Hockey.HockeyEvent;
import com.livescore.backend.Entity.Hockey.HockeyMatchState;
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

@Service("HOCKEY")
@RequiredArgsConstructor
public class HockeyScoringService implements ScoringServiceInterface {

    private final HockeyEventInterface hockeyEventInterface;
    private final HockeyMatchStateInterface hockeyMatchStateInterface;
    private final MatchInterface matchInterface;
    private final PlayerInterface playerInterface;
    private final TeamInterface teamInterface;
    private final HockeyStatsService hockeyStatsService;
    private final PlayerRequestInterface playerRequestInterface;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────
    // ScoringServiceInterface — 3 methods
    // ─────────────────────────────────────────

    @Override
    @Cacheable(value = "hockeyStates", key = "#matchId")
    @Transactional
    public Object getCurrentMatchState(Long matchId) {
        HockeyMatchState state = hockeyMatchStateInterface
                .findByMatch_Id(matchId)
                .orElseGet(() -> createInitialState(matchId));
        return toDTO(state, "");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CachePut(value = "hockeyStates", key = "#result.matchId")
    public Object scoring(JsonNode rawPayload) {
        HockeyScoreDTO score = objectMapper.convertValue(rawPayload, HockeyScoreDTO.class);
        return scoreHockey(score);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "hockeyStates", key = "#matchId")
    public Object undoLastBall(Long matchId, Long inningsId) {
        return undoLastEvent(matchId);
    }

    // ─────────────────────────────────────────
    // SCORING
    // ─────────────────────────────────────────

    private HockeyScoreDTO scoreHockey(HockeyScoreDTO score) {
        HockeyMatchState state = hockeyMatchStateInterface
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
            case "GREEN_CARD":
                score.setCardType("GREEN");
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
            case "PENALTY_CORNER":
                handlePenaltyCorner(score, state, match);
                break;
            case "END_PERIOD":
                handleEndPeriod(state, match);
                break;
            case "START_NEXT_PERIOD":
                handleStartNextPeriod(state);
                break;
            case "TIMEOUT":
                handleTimeout(score, state, match);
                break;
            default:
                throw new IllegalArgumentException("Unknown eventType: " + score.getEventType());
        }

        hockeyMatchStateInterface.save(state);
        return toDTO(state, "");
    }

    private void handleStartNextPeriod(HockeyMatchState state) {
        state.setStatus("LIVE");
        state.setPeriodStartTime(System.currentTimeMillis());
    }

    // ─────────────────────────────────────────
    // UNDO
    // ─────────────────────────────────────────

    private HockeyScoreDTO undoLastEvent(Long matchId) {
        HockeyEvent last = hockeyEventInterface
                .findTopByMatch_IdOrderByIdDesc(matchId).orElse(null);

        if (last == null) return (HockeyScoreDTO) getCurrentMatchState(matchId);

        HockeyMatchState state = hockeyMatchStateInterface.findByMatch_Id(matchId).get();

        switch (last.getEventType().toUpperCase()) {
            case "GOAL":
            case "OWN_GOAL":
                undoGoal(last, state);
                break;
            case "FOUL":
            case "GREEN_CARD":
            case "YELLOW_CARD":
            case "RED_CARD":
                undoFoul(last, state);
                break;
            case "END_PERIOD":
                undoEndPeriod(last, state);
                break;
            case "PENALTY_CORNER":
                undoPenaltyCorner(last, state);
                break;
            case "SUBSTITUTION":
                undoSubstitution(last, state, last.getMatch());
                break;
            // TIMEOUT: just delete event, no state rollback
        }

        hockeyEventInterface.delete(last);
        hockeyMatchStateInterface.save(state);
        return toDTO(state, "UNDO");
    }

    // ─────────────────────────────────────────
    // HANDLERS
    // ─────────────────────────────────────────

    private void handleGoal(HockeyScoreDTO score, HockeyMatchState state, Match match, boolean forceOwnGoal) {
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

        HockeyEvent ev = buildEvent(match, team, player,
                ownGoal ? "OWN_GOAL" : "GOAL", state);
        ev.setGoalType(score.getGoalType() != null ? score.getGoalType() : "NORMAL");
        ev.setExtraTime(state.getInExtraTime());

        // Assist
        if (score.getAssistPlayerId() != null && !ownGoal) {
            Player assist = playerInterface.findActiveById(score.getAssistPlayerId()).get();
            ev.setAssistPlayer(assist);
        }

        hockeyEventInterface.save(ev);
        hockeyStatsService.onEventSaved(ev);
    }

    private void handleFoul(HockeyScoreDTO score, HockeyMatchState state, Match match) {
        Player player = playerInterface.findActiveById(score.getPlayerId()).get();
        Team   team   = teamInterface.findById(score.getTeamId()).get();

        boolean isTeam1 = team.getId().equals(match.getTeam1().getId());

        // Always increment fouls for any disciplinary event
        if (isTeam1) state.setTeam1Fouls(state.getTeam1Fouls() + 1);
        else         state.setTeam2Fouls(state.getTeam2Fouls() + 1);

        // Card tracking (cards don't reset between periods)
        String cardType = score.getCardType();
        if ("GREEN".equalsIgnoreCase(cardType)) {
            if (isTeam1) state.setTeam1GreenCards(state.getTeam1GreenCards() + 1);
            else         state.setTeam2GreenCards(state.getTeam2GreenCards() + 1);
        } else if ("YELLOW".equalsIgnoreCase(cardType)) {
            if (isTeam1) state.setTeam1YellowCards(state.getTeam1YellowCards() + 1);
            else         state.setTeam2YellowCards(state.getTeam2YellowCards() + 1);
        } else if ("RED".equalsIgnoreCase(cardType)) {
            if (isTeam1) state.setTeam1RedCards(state.getTeam1RedCards() + 1);
            else         state.setTeam2RedCards(state.getTeam2RedCards() + 1);
        }

        // Determine eventType for storage
        String evType = "FOUL";
        if ("GREEN".equalsIgnoreCase(cardType))  evType = "GREEN_CARD";
        else if ("YELLOW".equalsIgnoreCase(cardType)) evType = "YELLOW_CARD";
        else if ("RED".equalsIgnoreCase(cardType))    evType = "RED_CARD";

        HockeyEvent ev = buildEvent(match, team, player, evType, state);
        ev.setCardType(cardType);
        ev.setExtraTime(state.getInExtraTime());
        hockeyEventInterface.save(ev);
        hockeyStatsService.onEventSaved(ev);
    }

    private void handlePenaltyCorner(HockeyScoreDTO score, HockeyMatchState state, Match match) {
        Team team = teamInterface.findById(score.getTeamId()).get();
        boolean isTeam1 = team.getId().equals(match.getTeam1().getId());

        if (isTeam1) state.setTeam1PenaltyCorners(state.getTeam1PenaltyCorners() + 1);
        else         state.setTeam2PenaltyCorners(state.getTeam2PenaltyCorners() + 1);

        // Player is optional for penalty corner
        Player player = score.getPlayerId() != null
                ? playerInterface.findActiveById(score.getPlayerId()).orElse(null)
                : null;

        HockeyEvent ev = buildEvent(match, team, player, "PENALTY_CORNER", state);
        ev.setExtraTime(state.getInExtraTime());
        hockeyEventInterface.save(ev);
        hockeyStatsService.onEventSaved(ev);
    }

    private void handleSubstitution(HockeyScoreDTO score, HockeyMatchState state, Match match) {
        Team team = teamInterface.findById(score.getTeamId()).get();
        Long outId = score.getOutPlayerId() != null ? score.getOutPlayerId() : score.getPlayerId();
        Long inId  = score.getInPlayerId();
        boolean isTeam1 = team.getId().equals(match.getTeam1().getId());

        if (isTeam1) state.setTeam1OnFieldIds(swapPlayer(state.getTeam1OnFieldIds(), outId, inId));
        else         state.setTeam2OnFieldIds(swapPlayer(state.getTeam2OnFieldIds(), outId, inId));

        HockeyEvent ev = buildEvent(match, team, null, "SUBSTITUTION", state);
        if (outId != null) ev.setPlayer(playerInterface.findActiveById(outId).orElse(null));
        if (inId  != null) ev.setInPlayer(playerInterface.findActiveById(inId).orElse(null));
        ev.setExtraTime(state.getInExtraTime());
        hockeyEventInterface.save(ev);
    }

    private void handleTimeout(HockeyScoreDTO score, HockeyMatchState state, Match match) {
        Team team = score.getTeamId() != null
                ? teamInterface.findById(score.getTeamId()).get() : null;
        HockeyEvent ev = buildEvent(match, team, null, "TIMEOUT", state);
        ev.setExtraTime(state.getInExtraTime());
        hockeyEventInterface.save(ev);
    }

    protected void handleEndPeriod(HockeyMatchState state, Match match) {
        int periodJustEnded = state.getCurrentPeriod();

        HockeyEvent ev = buildEvent(match, null, null, "END_PERIOD", state);
        ev.setPeriod(periodJustEnded);
        hockeyEventInterface.save(ev);

        if (state.getCurrentPeriod() == 1) {
            // First period ended → Break
            state.setStatus("BREAK");
            state.setCurrentPeriod(2);
            state.setPeriodStartTime(null);
            state.setInExtraTime(false);
        } else if (state.getCurrentPeriod() == 2) {
            // Second period ended → Break
            state.setStatus("BREAK");
            state.setCurrentPeriod(3);
            state.setPeriodStartTime(null);
        } else if (state.getCurrentPeriod() == 3) {
            // Third period ended — check scores
            if (state.getTeam1Score().equals(state.getTeam2Score())) {
                // Draw → Extra Time
                state.setStatus("EXTRA_TIME");
                state.setInExtraTime(true);
                state.setCurrentPeriod(4);
                state.setPeriodStartTime(null);
            } else {
                // Determine winner
                state.setStatus("COMPLETED");
                match.setStatus("COMPLETED");
                matchInterface.save(match);
                determineWinner(state, match);
                hockeyStatsService.onMatchEnd(match.getId(), state.getTeam1Score(), state.getTeam2Score());
                state.setPeriodStartTime(null);
            }
        } else {
            // Extra time (period == 4) ended → Completed
            state.setStatus("COMPLETED");
            match.setStatus("COMPLETED");
            matchInterface.save(match);
            determineWinner(state, match);
            hockeyStatsService.onMatchEnd(match.getId(), state.getTeam1Score(), state.getTeam2Score());
            state.setPeriodStartTime(null);
        }
    }

    // ─────────────────────────────────────────
    // UNDO HELPERS
    // ─────────────────────────────────────────

    private void undoGoal(HockeyEvent last, HockeyMatchState state) {
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

    private void undoFoul(HockeyEvent last, HockeyMatchState state) {
        boolean isTeam1 = last.getTeam().getId().equals(last.getMatch().getTeam1().getId());

        if (isTeam1) state.setTeam1Fouls(Math.max(0, state.getTeam1Fouls() - 1));
        else         state.setTeam2Fouls(Math.max(0, state.getTeam2Fouls() - 1));

        String card = last.getCardType();
        if ("GREEN".equalsIgnoreCase(card)) {
            if (isTeam1) state.setTeam1GreenCards(Math.max(0, state.getTeam1GreenCards() - 1));
            else         state.setTeam2GreenCards(Math.max(0, state.getTeam2GreenCards() - 1));
        } else if ("YELLOW".equalsIgnoreCase(card)) {
            if (isTeam1) state.setTeam1YellowCards(Math.max(0, state.getTeam1YellowCards() - 1));
            else         state.setTeam2YellowCards(Math.max(0, state.getTeam2YellowCards() - 1));
        } else if ("RED".equalsIgnoreCase(card)) {
            if (isTeam1) state.setTeam1RedCards(Math.max(0, state.getTeam1RedCards() - 1));
            else         state.setTeam2RedCards(Math.max(0, state.getTeam2RedCards() - 1));
        }
    }

    private void undoPenaltyCorner(HockeyEvent last, HockeyMatchState state) {
        boolean isTeam1 = last.getTeam().getId().equals(last.getMatch().getTeam1().getId());
        if (isTeam1) state.setTeam1PenaltyCorners(Math.max(0, state.getTeam1PenaltyCorners() - 1));
        else         state.setTeam2PenaltyCorners(Math.max(0, state.getTeam2PenaltyCorners() - 1));
    }

    private void undoEndPeriod(HockeyEvent last, HockeyMatchState state) {
        state.setCurrentPeriod(last.getPeriod());
        state.setStatus("LIVE");
        state.setInExtraTime(false);
    }

    private void undoSubstitution(HockeyEvent last, HockeyMatchState state, Match match) {
        if (last.getPlayer() == null || last.getInPlayer() == null) return;
        Long outId = last.getPlayer().getId();    // was removed from field
        Long inId  = last.getInPlayer().getId();  // was added to field
        boolean isTeam1 = last.getTeam().getId().equals(match.getTeam1().getId());
        // Reverse the swap: remove inPlayer, restore outPlayer
        if (isTeam1) state.setTeam1OnFieldIds(swapPlayer(state.getTeam1OnFieldIds(), inId, outId));
        else         state.setTeam2OnFieldIds(swapPlayer(state.getTeam2OnFieldIds(), inId, outId));
    }

    // ─────────────────────────────────────────
    // UTILITY
    // ─────────────────────────────────────────

    private HockeyEvent buildEvent(Match match, Team team, Player player,
                                   String type, HockeyMatchState state) {
        HockeyEvent ev = new HockeyEvent();
        ev.setMatch(match);
        ev.setTeam(team);
        ev.setPlayer(player);
        ev.setEventType(type);
        ev.setPeriod(state.getCurrentPeriod());
        ev.setEventTimeSeconds(calcElapsedSeconds(state));
        return ev;
    }

    private int calcElapsedSeconds(HockeyMatchState state) {
        if (state.getPeriodStartTime() == null) return 0;
        return (int) ((System.currentTimeMillis() - state.getPeriodStartTime()) / 1000);
    }

    private void determineWinner(HockeyMatchState state, Match match) {
        if (state.getTeam1Score() > state.getTeam2Score())
            match.setWinnerTeam(match.getTeam1());
        else if (state.getTeam2Score() > state.getTeam1Score())
            match.setWinnerTeam(match.getTeam2());
        // draw: winnerTeam stays null

        match.setStatus("COMPLETED");
        matchInterface.save(match);
    }

    private HockeyMatchState createInitialState(Long matchId) {
        Match match = matchInterface.findById(matchId).get();
        HockeyMatchState s = new HockeyMatchState();
        s.setMatch(match);
        s.setTeam1Score(0);   s.setTeam2Score(0);
        s.setTeam1Fouls(0);   s.setTeam2Fouls(0);
        s.setTeam1YellowCards(0); s.setTeam2YellowCards(0);
        s.setTeam1RedCards(0);    s.setTeam2RedCards(0);
        s.setTeam1GreenCards(0);  s.setTeam2GreenCards(0);
        s.setTeam1PenaltyCorners(0); s.setTeam2PenaltyCorners(0);
        s.setCurrentPeriod(1);
        s.setStatus("LIVE");
        s.setInExtraTime(false);
        s.setPeriodStartTime(System.currentTimeMillis());
        // Default 15 min per period; use match config if available
        s.setPeriodDurationMinutes(match.getHalfDurationMins() != null ? match.getHalfDurationMins() : 15);
        // Lineup from match entity
        if (match.getTeam1PlayingIds() != null && !match.getTeam1PlayingIds().isBlank())
            s.setTeam1OnFieldIds(match.getTeam1PlayingIds());
        if (match.getTeam2PlayingIds() != null && !match.getTeam2PlayingIds().isBlank())
            s.setTeam2OnFieldIds(match.getTeam2PlayingIds());
        return hockeyMatchStateInterface.save(s);
    }

    // ─────────────────────────────────────────
    // DTO CONVERTERS
    // ─────────────────────────────────────────

    private HockeyScoreDTO toDTO(HockeyMatchState state, String comment) {
        HockeyScoreDTO dto = new HockeyScoreDTO();
        dto.setMatchId(state.getMatch().getId());
        dto.setTeam1Score(state.getTeam1Score());
        dto.setTeam2Score(state.getTeam2Score());
        dto.setTeam1Fouls(state.getTeam1Fouls());
        dto.setTeam2Fouls(state.getTeam2Fouls());
        dto.setTeam1YellowCards(state.getTeam1YellowCards());
        dto.setTeam2YellowCards(state.getTeam2YellowCards());
        dto.setTeam1RedCards(state.getTeam1RedCards());
        dto.setTeam2RedCards(state.getTeam2RedCards());
        dto.setTeam1GreenCards(state.getTeam1GreenCards());
        dto.setTeam2GreenCards(state.getTeam2GreenCards());
        dto.setTeam1PenaltyCorners(state.getTeam1PenaltyCorners());
        dto.setTeam2PenaltyCorners(state.getTeam2PenaltyCorners());
        dto.setCurrentPeriod(state.getCurrentPeriod());
        dto.setStatus(state.getStatus());
        dto.setInExtraTime(state.getInExtraTime() != null && state.getInExtraTime());
        dto.setPeriodStartTime(state.getPeriodStartTime());
        dto.setPeriodDurationMinutes(state.getPeriodDurationMinutes() != null ? state.getPeriodDurationMinutes() : 15);
        dto.setComment(comment);

        List<HockeyEvent> events = hockeyEventInterface
                .findByMatch_IdOrderByIdAsc(state.getMatch().getId());
        dto.setHockeyEvents(events.stream().map(this::eventToDTO).collect(Collectors.toList()));

        Match match = state.getMatch();
        List<Player> squad1 = playerRequestInterface.findApprovedPlayersByTeamId(match.getTeam1().getId());
        List<Player> squad2 = playerRequestInterface.findApprovedPlayersByTeamId(match.getTeam2().getId());
        dto.setTeam1Players(toSimpleDTOs(squad1));
        dto.setTeam2Players(toSimpleDTOs(squad2));
        dto.setTeam1OnField(resolveOnField(squad1, state.getTeam1OnFieldIds()));
        dto.setTeam2OnField(resolveOnField(squad2, state.getTeam2OnFieldIds()));
        return dto;
    }

    private HockeyEventDTO eventToDTO(HockeyEvent ev) {
        HockeyEventDTO dto = new HockeyEventDTO();
        dto.setId(ev.getId());
        dto.setEventType(ev.getEventType());
        dto.setGoalType(ev.getGoalType());
        dto.setCardType(ev.getCardType());
        dto.setPeriod(ev.getPeriod());
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
