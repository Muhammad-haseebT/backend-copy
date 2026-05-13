package com.livescore.backend.Sport.TableTennis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.PlayerSimpleDTO;
import com.livescore.backend.DTO.ScoringDTOs.TableTennisEventDTO;
import com.livescore.backend.DTO.ScoringDTOs.TableTennisScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Entity.TableTennis.TableTennisEvent;
import com.livescore.backend.Entity.TableTennis.TableTennisMatchState;
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

@Service("TABLETENNIS")
@RequiredArgsConstructor
public class TableTennisScoringService implements ScoringServiceInterface {

    private final TableTennisEventInterface      ttEventInterface;
    private final TableTennisMatchStateInterface ttStateInterface;
    private final MatchInterface                 matchInterface;
    private final PlayerInterface                playerInterface;
    private final TeamInterface                  teamInterface;
    private final TableTennisStatsService        ttStatsService;
    private final TableTennisPtsTableService     ttPtsTableService;
    private final PlayerRequestInterface playerRequestInterface;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int WIN_BY = 2; // must win by 2

    // ─────────────────────────────────────────
    @Override
    @Cacheable(value = "ttStates", key = "#matchId")
    @Transactional
    public Object getCurrentMatchState(Long matchId) {
        TableTennisMatchState state = ttStateInterface.findByMatch_Id(matchId)
                .orElseGet(() -> createInitialState(matchId));
        return toDTO(state, "");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CachePut(value = "ttStates", key = "#result.matchId")
    public Object scoring(JsonNode rawPayload) {
        TableTennisScoreDTO req = objectMapper.convertValue(rawPayload, TableTennisScoreDTO.class);
        return process(req);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = "ttStates", key = "#matchId")
    public Object undoLastBall(Long matchId, Long unused) {
        return undoLast(matchId);
    }

    // ─────────────────────────────────────────
    private TableTennisScoreDTO process(TableTennisScoreDTO req) {
        TableTennisMatchState state = ttStateInterface.findByMatch_Id(req.getMatchId())
                .orElseGet(() -> createInitialState(req.getMatchId()));
        Match match = matchInterface.findById(req.getMatchId()).get();

        TableTennisEvent saved = null;

        switch (req.getEventType().toUpperCase()) {
            // ── Scoring team gets +1 ─────────────────────────────
            case "POINT", "SMASH", "SERVICE_ACE", "EDGE_BALL"
                -> saved = handleScoringEvent(req, state, match, req.getEventType().toUpperCase());
            // ── Fault → opponent gets +1 ─────────────────────────
            case "NET_FAULT", "OUT", "SERVICE_FAULT"
                -> saved = handleFaultEvent(req, state, match, req.getEventType().toUpperCase());
            case "END_GAME"
                -> handleEndGame(state, match);
            default -> throw new IllegalArgumentException("Unknown event: " + req.getEventType());
        }

        ttStateInterface.save(state);
        if (saved != null) ttStatsService.onEventSaved(saved);
        if ("COMPLETED".equals(state.getStatus())) {
            ttPtsTableService.updateAfterMatch(match.getId());
            ttStatsService.onMatchEnd(match.getId());
        }
        return toDTO(state, "");
    }

    // ─────────────────────────────────────────
    private TableTennisEvent handleScoringEvent(TableTennisScoreDTO req,
            TableTennisMatchState state, Match match, String type) {
        Team team   = teamInterface.findById(req.getTeamId()).get();
        Player plyr = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;
        addPoint(team, match, state);
        TableTennisEvent ev = build(match, team, plyr, type, state);
        TableTennisEvent saved = ttEventInterface.save(ev);
        checkGameComplete(state, match);
        return saved;
    }

    private TableTennisEvent handleFaultEvent(TableTennisScoreDTO req,
            TableTennisMatchState state, Match match, String type) {
        Team faultTeam = teamInterface.findById(req.getTeamId()).get();
        Player plyr    = req.getPlayerId() != null
                ? playerInterface.findActiveById(req.getPlayerId()).orElse(null) : null;
        Team opponent  = faultTeam.getId().equals(match.getTeam1().getId())
                ? match.getTeam2() : match.getTeam1();
        addPoint(opponent, match, state);
        TableTennisEvent ev = build(match, faultTeam, plyr, type, state);
        TableTennisEvent saved = ttEventInterface.save(ev);
        checkGameComplete(state, match);
        return saved;
    }

    private void handleEndGame(TableTennisMatchState state, Match match) {
        TableTennisEvent ev = build(match, null, null, "END_GAME", state);
        ev.setGameNumber(state.getCurrentGame());
        ttEventInterface.save(ev);
        if (state.getTeam1Points() > state.getTeam2Points())
            state.setTeam1Games(state.getTeam1Games() + 1);
        else if (state.getTeam2Points() > state.getTeam1Points())
            state.setTeam2Games(state.getTeam2Games() + 1);
        checkMatchOver(state, match);
    }

    // ─── TT game win rules ────────────────────────────────────────
    // First to 11, must win by 2, NO cap (deuce continues forever)
    private void checkGameComplete(TableTennisMatchState state, Match match) {
        int pts = safeVal(state.getPointsPerGame(), 11);
        int t1  = state.getTeam1Points();
        int t2  = state.getTeam2Points();

        int maxPts = safeVal(state.getMaxPoints(), 0);
        boolean t1Wins, t2Wins;

        if (maxPts > 0) {
            // Capped (e.g. badminton: max 30)
            t1Wins = (t1 >= pts && (t1 - t2) >= WIN_BY) || t1 >= maxPts;
            t2Wins = (t2 >= pts && (t2 - t1) >= WIN_BY) || t2 >= maxPts;
        } else {
            // No cap — true deuce (table tennis standard)
            t1Wins = t1 >= pts && (t1 - t2) >= WIN_BY;
            t2Wins = t2 >= pts && (t2 - t1) >= WIN_BY;
        }

        if (!t1Wins && !t2Wins) return;

        TableTennisEvent ev = build(match, null, null, "END_GAME", state);
        ev.setGameNumber(state.getCurrentGame());
        ev.setScoreSnapshot(t1 + "-" + t2);
        ttEventInterface.save(ev);

        if (t1Wins) state.setTeam1Games(state.getTeam1Games() + 1);
        else        state.setTeam2Games(state.getTeam2Games() + 1);

        checkMatchOver(state, match);
    }


    private void checkMatchOver(TableTennisMatchState state, Match match) {
        if (state.getTeam1Games() >= state.getGamesToWin()
                || state.getTeam2Games() >= state.getGamesToWin()) {
            state.setStatus("COMPLETED");
            if (state.getTeam1Games() > state.getTeam2Games()) match.setWinnerTeam(match.getTeam1());
            else                                                match.setWinnerTeam(match.getTeam2());
            matchInterface.save(match);
        } else {
            state.setCurrentGame(state.getCurrentGame() + 1);
            state.setTeam1Points(0); state.setTeam2Points(0);
            state.setGameStartTime(System.currentTimeMillis());
            state.setStatus("LIVE");
        }
    }

    // ─────────────────────────────────────────
    private TableTennisScoreDTO undoLast(Long matchId) {
        TableTennisEvent last = ttEventInterface.findTopByMatch_IdOrderByIdDesc(matchId).orElse(null);
        if (last == null) return (TableTennisScoreDTO) getCurrentMatchState(matchId);

        TableTennisMatchState state = ttStateInterface.findByMatch_Id(matchId).get();
        switch (last.getEventType().toUpperCase()) {
            case "POINT", "SMASH", "SERVICE_ACE", "EDGE_BALL"
                -> undoPoint(last, state, false);
            case "NET_FAULT", "OUT", "SERVICE_FAULT"
                -> undoPoint(last, state, true);
            case "END_GAME"
                -> undoEndGame(state);
        }
        ttEventInterface.delete(last);
        ttStateInterface.save(state);
        if (last.getPlayer() != null && last.getMatch().getTournament() != null)
            ttStatsService.recalculatePlayerStats(last.getPlayer().getId(),
                    last.getMatch().getTournament().getId());
        return toDTO(state, "UNDO");
    }

    private void undoPoint(TableTennisEvent last, TableTennisMatchState state, boolean fault) {
        if (last.getTeam() == null) return;
        boolean lastT1 = last.getTeam().getId().equals(last.getMatch().getTeam1().getId());
        boolean ptT1   = fault ? !lastT1 : lastT1;
        if (ptT1) state.setTeam1Points(Math.max(0, state.getTeam1Points() - 1));
        else      state.setTeam2Points(Math.max(0, state.getTeam2Points() - 1));
    }

    private void undoEndGame(TableTennisMatchState state) {
        if (state.getCurrentGame() > 1) state.setCurrentGame(state.getCurrentGame() - 1);
        state.setTeam1Points(0); state.setTeam2Points(0); state.setStatus("LIVE");
        if (state.getTeam1Games() > 0) state.setTeam1Games(state.getTeam1Games() - 1);
        else if (state.getTeam2Games() > 0) state.setTeam2Games(state.getTeam2Games() - 1);
    }

    // ─────────────────────────────────────────
    private void addPoint(Team team, Match match, TableTennisMatchState state) {
        if (team.getId().equals(match.getTeam1().getId()))
            state.setTeam1Points(state.getTeam1Points() + 1);
        else
            state.setTeam2Points(state.getTeam2Points() + 1);
    }

    private TableTennisEvent build(Match match, Team team, Player player,
                                    String type, TableTennisMatchState state) {
        TableTennisEvent ev = new TableTennisEvent();
        ev.setMatch(match); ev.setTeam(team); ev.setPlayer(player);
        ev.setEventType(type); ev.setGameNumber(state.getCurrentGame());
        ev.setEventTimeSeconds(state.getGameStartTime() != null
                ? (int) ((System.currentTimeMillis() - state.getGameStartTime()) / 1000) : 0);
        ev.setScoreSnapshot(state.getTeam1Points() + "-" + state.getTeam2Points());
        return ev;
    }

    private TableTennisMatchState createInitialState(Long matchId) {
        Match match = matchInterface.findById(matchId).get();
        TableTennisMatchState s = new TableTennisMatchState();
        s.setMatch(match);
        s.setTeam1Points(0); s.setTeam2Points(0);
        s.setTeam1Games(0);  s.setTeam2Games(0);
        s.setCurrentGame(1); s.setStatus("LIVE");
        s.setGameStartTime(System.currentTimeMillis());
        // Config from match — same fields as volleyball/badminton
        s.setGamesToWin(match.getSets() != 0 && match.getSets() > 0 ? match.getSets() : 4);
        s.setPointsPerGame(match.getPointsPerSet() != null ? match.getPointsPerSet() : 11);
        // maxPoints: 0 = no cap (true TT deuce); use finalSetPoints if set
        s.setMaxPoints(match.getFinalSetPoints() != null ? match.getFinalSetPoints() : 0);
        if (match.getTeam1PlayingIds() != null && !match.getTeam1PlayingIds().isBlank())
            s.setTeam1PlayerIds(match.getTeam1PlayingIds());
        if (match.getTeam2PlayingIds() != null && !match.getTeam2PlayingIds().isBlank())
            s.setTeam2PlayerIds(match.getTeam2PlayingIds());
        return ttStateInterface.save(s);
    }

    private int safeVal(Integer v, int def) { return v != null ? v : def; }

    private TableTennisScoreDTO toDTO(TableTennisMatchState state, String comment) {
        TableTennisScoreDTO dto = new TableTennisScoreDTO();
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

        // Dynamic pointsToWin — extends at deuce
        int pts = safeVal(state.getPointsPerGame(), 11);
        int higher = Math.max(state.getTeam1Points(), state.getTeam2Points());
        int toWin  = higher >= pts - 1
                ? higher + WIN_BY
                : pts;
        int maxPts = safeVal(state.getMaxPoints(), 0);
        if (maxPts > 0) toWin = Math.min(toWin, maxPts);
        dto.setPointsToWin(toWin);

        dto.setComment(comment);
        dto.setTableTennisEvents(ttEventInterface.findByMatch_IdOrderByIdAsc(state.getMatch().getId())
                .stream().map(this::toEventDTO).collect(Collectors.toList()));
        Match m = state.getMatch();
        List<Player> all1 = playerRequestInterface.findApprovedPlayersByTeamId(m.getTeam1().getId());
        List<Player> all2 = playerRequestInterface.findApprovedPlayersByTeamId(m.getTeam2().getId());
        dto.setTeam1Players(resolvePlayingPlayers(all1, state.getTeam1PlayerIds()));
        dto.setTeam2Players(resolvePlayingPlayers(all2, state.getTeam2PlayerIds()));
        return dto;
    }

    private TableTennisEventDTO toEventDTO(TableTennisEvent ev) {
        TableTennisEventDTO dto = new TableTennisEventDTO();
        dto.setId(ev.getId()); dto.setEventType(ev.getEventType());
        dto.setGameNumber(ev.getGameNumber());
        dto.setEventTimeSeconds(ev.getEventTimeSeconds());
        dto.setScoreSnapshot(ev.getScoreSnapshot());
        if (ev.getPlayer() != null) { dto.setPlayerId(ev.getPlayer().getId()); dto.setPlayerName(ev.getPlayer().getName()); }
        if (ev.getTeam()   != null) { dto.setTeamId(ev.getTeam().getId());     dto.setTeamName(ev.getTeam().getName()); }
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
