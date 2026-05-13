package com.livescore.backend.Sport.Cricket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.DTO.PlayerSimpleDTO;
import com.livescore.backend.DTO.PlayerStatDTO;
import com.livescore.backend.DTO.ScoringDTOs.ScoreDTO;
import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.*;
import com.livescore.backend.Interface.Cricket.MatchStateInterface;
import com.livescore.backend.Interface.Cricket.PlayerInningsInterface;
import com.livescore.backend.Interface.multisportgeneric.ScoringServiceInterface;
import com.livescore.backend.Service.MatchService;
import com.livescore.backend.Service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service("CRICKET") // ← sport name as bean name
public class CricketScoringService implements ScoringServiceInterface {
    private final MatchStateInterface matchStateInterface;
    private final PlayerInningsInterface playerInningsInterface;
    private final CricketInningsInterface cricketInningsInterface;
    private final StatsService statsService;
    private final PlayerInterface playerInterface;
    private final CricketBallInterface cricketBallInterface;
    private final MatchInterface matchInterface;
    private final MatchService matchService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlayerRequestInterface playerRequestInterface;


    @Transactional(readOnly = true)
    @Cacheable(value = "matchStates", key = "#matchId")
    public Object getCurrentMatchState(Long matchId) {
        Match m = matchInterface.findById(matchId).get();
        int size = m.getCricketInnings().size();
        MatchState state = matchStateInterface.findByInnings_Id(m.getCricketInnings().get(size - 1).getId());
        if (state == null) {
            ScoreDTO s = new ScoreDTO();
            s.setMatchId(matchId);
            s.setComment("Error no current State");
            s.setInningsId(m.getCricketInnings().get(size - 1).getId());
            return s;
        }

        return convertToScoreDTO(state, false, false, "");
    }

    private ScoreDTO convertToScoreDTO(MatchState state, Boolean rotate, boolean a, String b) {
        ScoreDTO scoreDTO = new ScoreDTO();

        scoreDTO.setMatchId(state.getInnings().getMatch().getId());
        scoreDTO.setInningsId(state.getInnings().getId());
        scoreDTO.setWickets(state.getWickets());
        scoreDTO.setRuns(state.getRuns());
        scoreDTO.setBalls(state.getBalls());
        scoreDTO.setOvers(state.getOvers());
        scoreDTO.setStatus(state.getStatus());
        scoreDTO.setCrr(state.getCrr());
        scoreDTO.setTarget(state.getTarget());
        scoreDTO.setExtra(state.getExtras());

        // ── Super Over + firstInnings detection ──────────────────────
        boolean isSuperOver = state.getInnings().isSuper_Over();
        scoreDTO.setSuperOver(isSuperOver);

        int inningsNo = state.getInnings().getNo();
        if (isSuperOver) {
            // odd no = SO 1st innings, even no = SO 2nd innings
            scoreDTO.setFirstInnings(inningsNo % 2 == 1);
        } else {
            scoreDTO.setFirstInnings(inningsNo == 1);
        }
        // ─────────────────────────────────────────────────────────────

        if (!a) {
            PlayerStatDTO batsmanDto   = new PlayerStatDTO();
            PlayerStatDTO nonStrikerDto = new PlayerStatDTO();
            PlayerStatDTO bowlerDto    = new PlayerStatDTO();

            PlayerInnings batsman    = playerInningsInterface.findByInnings_IdAndPlayer_Id(state.getInnings().getId(), state.getStriker().getId());
            PlayerInnings bowler     = playerInningsInterface.findByInnings_IdAndPlayer_Id(state.getInnings().getId(), state.getBowler().getId());
            PlayerInnings nonStriker = playerInningsInterface.findByInnings_IdAndPlayer_Id(state.getInnings().getId(), state.getNonStriker().getId());

            if (batsman != null && batsman.getPlayer() != null) {
                scoreDTO.setBatsmanId(batsman.getPlayer().getId());
                batsmanDto.setRuns(batsman.getRuns());
                batsmanDto.setSixes(batsman.getSixes());
                batsmanDto.setFours(batsman.getFour());
                batsmanDto.setBallsFaced(batsman.getBallsFaced());
                batsmanDto.setPlayerId(batsman.getPlayer().getId());
                batsmanDto.setPlayerName(batsman.getPlayer().getName());
                batsmanDto.setStrikeRate(batsman.getBallsFaced() > 0
                        ? (batsman.getRuns() * 100.0) / batsman.getBallsFaced() : 0);
            }
            if (nonStriker != null && nonStriker.getPlayer() != null) {
                scoreDTO.setNonStrikerId(nonStriker.getPlayer().getId());
                nonStrikerDto.setRuns(nonStriker.getRuns());
                nonStrikerDto.setSixes(nonStriker.getSixes());
                nonStrikerDto.setFours(nonStriker.getFour());
                nonStrikerDto.setBallsFaced(nonStriker.getBallsFaced());
                nonStrikerDto.setPlayerId(nonStriker.getPlayer().getId());
                nonStrikerDto.setPlayerName(nonStriker.getPlayer().getName());
                nonStrikerDto.setStrikeRate(nonStriker.getBallsFaced() > 0
                        ? (nonStriker.getRuns() * 100.0) / nonStriker.getBallsFaced() : 0);
            }
            if (bowler != null && bowler.getPlayer() != null) {
                scoreDTO.setBowlerId(bowler.getPlayer().getId());
                bowlerDto.setRunsConceded(bowler.getRunsConceded());
                bowlerDto.setWickets(bowler.getWickets());
                bowlerDto.setBallsBowled(bowler.getBallsBowled());
                bowlerDto.setPlayerId(bowler.getPlayer().getId());
                bowlerDto.setPlayerName(bowler.getPlayer().getName());
                bowlerDto.setEconomy(bowler.getBallsBowled() > 0
                        ? (bowler.getRunsConceded() * 6.0) / bowler.getBallsBowled() : 0);
            }
            if (rotate) {
                scoreDTO.setBatsmanId(nonStriker.getPlayer().getId());
                scoreDTO.setNonStrikerId(batsman.getPlayer().getId());
            }

            scoreDTO.setBatsman1Stats(batsmanDto);
            scoreDTO.setBatsman2Stats(nonStrikerDto);
            scoreDTO.setBowlerStats(bowlerDto);
        }

        if (!a) {
            int maxBalls = matchInterface.findById(scoreDTO.getMatchId()).get().getOvers() * 6;
            int balls    = scoreDTO.getOvers() * 6 + scoreDTO.getBalls();

            if (!isSuperOver) {
                if (scoreDTO.isFirstInnings() && (balls >= maxBalls || scoreDTO.getWickets() == 10)) {
                    scoreDTO.setComment("End_Innings");
                    state.setTarget(state.getTarget() + 1);
                    matchStateInterface.save(state);
                }
                if (!scoreDTO.isFirstInnings()) {
                    // ✅ FIX: target <= 0 matlab jeet gaye, innings over
                    boolean inningsOver = scoreDTO.getWickets() == 10
                            || balls >= maxBalls
                            || scoreDTO.getTarget() <= 0;  // ← WAS: <= 1
                    if (inningsOver) {
                        if (scoreDTO.getTarget() == 1) {
                            scoreDTO.setComment("Super_Over"); // TIE — innings khatam aur score equal
                        } else {
                            scoreDTO.setComment("End_Innings"); // WIN or LOSS
                        }
                    }
                }
            } else {
                // Super Over same fix
                if (scoreDTO.isFirstInnings() && (balls >= maxBalls || scoreDTO.getWickets() == 10)) {
                    scoreDTO.setComment("End_Innings");
                    state.setTarget(state.getTarget() + 1);
                    matchStateInterface.save(state);
                }
                if (!scoreDTO.isFirstInnings()) {
                    // ✅ FIX: same — innings khatam tab hi decide karo
                    boolean inningsOver = scoreDTO.getWickets() == 10
                            || balls >= maxBalls
                            || scoreDTO.getTarget() <= 0;  // ← WAS: <= 1
                    if (inningsOver) {
                        if (scoreDTO.getTarget() == 1) {
                            scoreDTO.setComment("Super_Over"); // Another SO tie
                        } else {
                            scoreDTO.setComment("End_Innings");
                        }
                    }
                }
            }

            scoreDTO.setCricketBalls(
                    cricketBallInterface.getBalls(scoreDTO.getInningsId(), scoreDTO.getMatchId(), isSuperOver)
            );
        }
        try {
            Long battingTeamId = state.getInnings().getTeam().getId();
            Match m2 = state.getInnings().getMatch();
            Long bowlingTeamId = m2.getTeam1().getId().equals(battingTeamId)
                    ? m2.getTeam2().getId()
                    : m2.getTeam1().getId();

            // Players dismissed this innings
            Set<Long> dismissedIds = playerInningsInterface
                    .findByInnings_Id(state.getInnings().getId())
                    .stream()
                    .filter(pi -> Boolean.TRUE.equals(pi.getDismissed()))
                    .map(pi -> pi.getPlayer().getId())
                    .collect(java.util.stream.Collectors.toSet());

            // Current on-crease players — excluded from "new batsman" list
            Set<Long> onCrease = new java.util.HashSet<>();
            if (state.getStriker()    != null) onCrease.add(state.getStriker().getId());
            if (state.getNonStriker() != null) onCrease.add(state.getNonStriker().getId());

            // availableBatters = batting team - dismissed - on crease
            List<PlayerSimpleDTO> batters = playerRequestInterface
                    .findApprovedPlayersByTeamId(battingTeamId)
                    .stream()
                    .filter(p -> !dismissedIds.contains(p.getId()) && !onCrease.contains(p.getId()))
                    .map(p -> new PlayerSimpleDTO(p.getId(), p.getName()))
                    .collect(Collectors.toList());
            scoreDTO.setAvailableBatters(batters);

            // availableBowlers = bowling team - last over bowler
            Long lastOverBowlerId = state.getLastOverBowlerId();
            List<PlayerSimpleDTO> bowlers = playerRequestInterface
                    .findApprovedPlayersByTeamId(bowlingTeamId)
                    .stream()
                    .filter(p -> !p.getId().equals(lastOverBowlerId))
                    .map(p -> new PlayerSimpleDTO(p.getId(), p.getName()))
                    .collect(Collectors.toList());
            scoreDTO.setAvailableBowlers(bowlers);

        } catch (Exception e) {
            // Non-critical — scoring still works, just no bench data
            scoreDTO.setAvailableBatters(java.util.Collections.emptyList());
            scoreDTO.setAvailableBowlers(java.util.Collections.emptyList());
        }
        scoreDTO.setMatchEnd(state.getMatchEnd());
        return scoreDTO;
    }

    @CacheEvict(value = "matchStates", key = "#matchId")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object undoLastBall(Long matchId, Long inningsId) {
        CricketBall cb = cricketBallInterface.findLastBallInInnings(inningsId);
        if (cb == null) {
            return getCurrentMatchState(matchId);
        }

        MatchState m = matchStateInterface.findByInnings_Id(inningsId);
        PlayerInnings batsman = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, cb.getBatsman().getId());
        PlayerInnings nonStriker = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, cb.getNonStriker().getId());
        PlayerInnings bowler = playerInningsInterface.findByInnings_IdAndPlayer_Id(inningsId, cb.getBowler().getId());


        // grab IDs BEFORE delete for stats recalculation
        Long undoBatsmanId = cb.getBatsman() != null ? cb.getBatsman().getId() : null;
        Long undoBowlerId = cb.getBowler() != null ? cb.getBowler().getId() : null;
        Long undoFielderId = cb.getFielder() != null ? cb.getFielder().getId() : null;
        Long tournamentId = cb.getMatch().getTournament().getId();

        int r = Integer.parseInt(cb.getEvent());
        Boolean a=cb.getInnings().getNo()==1?true:false;

        switch (cb.getEventType()) {
            case "run":
            case "boundary":
                batsman.setRuns(batsman.getRuns() - r);
                batsman.setBallsFaced(batsman.getBallsFaced() - 1);
                if (r == 4) batsman.setFour(batsman.getFour() - 1);
                else if (r == 6) batsman.setSixes(batsman.getSixes() - 1);
                bowler.setRunsConceded(bowler.getRunsConceded() - r);
                bowler.setBallsBowled(bowler.getBallsBowled() - 1);
                batsman.setRr(batsman.getBallsFaced() > 0
                        ? (double) batsman.getRuns() / batsman.getBallsFaced() : 0);
                bowler.setEco(bowler.getBallsBowled() > 0
                        ? (double) bowler.getRunsConceded() / bowler.getBallsBowled() : 0);
                m.setRuns(m.getRuns() - r);
                if (m.getBalls() == 0) m.setLastOverBowlerId(null);
                decrementBall(m);

                if(a)
                m.setTarget(m.getTarget() - r);
                else
                    m.setTarget(m.getTarget() + r);

                break;

            case "bye":
            case "legbye":
                batsman.setBallsFaced(batsman.getBallsFaced() - 1);
                bowler.setBallsBowled(bowler.getBallsBowled() - 1);
                batsman.setRr(batsman.getBallsFaced() > 0
                        ? (double) batsman.getRuns() / batsman.getBallsFaced() : 0);
                bowler.setEco(bowler.getBallsBowled() > 0
                        ? (double) bowler.getRunsConceded() / bowler.getBallsBowled() : 0);
                m.setExtras(m.getExtras() - r);
                m.setRuns(m.getRuns() - r);
                if (m.getBalls() == 0) m.setLastOverBowlerId(null);
                decrementBall(m);
                if(a)
                    m.setTarget(m.getTarget() - r);
                else
                    m.setTarget(m.getTarget() + r);

                break;

            case "noball":
                m.setExtras(m.getExtras() - 1);
                batsman.setRuns(batsman.getRuns() - r);
                batsman.setBallsFaced(batsman.getBallsFaced() - 1);
                bowler.setRunsConceded(bowler.getRunsConceded() - r - 1);
                if (r == 4) batsman.setFour(batsman.getFour() - 1);
                if (r == 6) batsman.setSixes(batsman.getSixes() - 1);
                batsman.setRr(batsman.getBallsFaced() > 0
                        ? (double) batsman.getRuns() / batsman.getBallsFaced() : 0);
                bowler.setEco(bowler.getBallsBowled() > 0
                        ? (double) bowler.getRunsConceded() / bowler.getBallsBowled() : 0);
                m.setRuns(m.getRuns() - r - 1);
                if(a)
                    m.setTarget(m.getTarget() - r);
                else
                    m.setTarget(m.getTarget() + r);

                break;

            case "wide":
                m.setExtras(m.getExtras() - r - 1);
                bowler.setRunsConceded(bowler.getRunsConceded() - r - 1);
                bowler.setEco(bowler.getBallsBowled() > 0
                        ? (double) bowler.getRunsConceded() / bowler.getBallsBowled() : 0);
                m.setRuns(m.getRuns() - r - 1);
                if(a)
                    m.setTarget(m.getTarget() - r-1);
                else
                    m.setTarget(m.getTarget() + r+1);

                break;

            case "wicket":
                handleUndoWicket(m, batsman, bowler, nonStriker, cb);
                if(a)
                    m.setTarget(m.getTarget() - r);
                else
                    m.setTarget(m.getTarget() + r);


                break;
            case "penalty":
                m.setRuns(m.getRuns() - r);
                if(a)
                    m.setTarget(m.getTarget() - r);
                else
                    m.setTarget(m.getTarget() + r);

                break;
        }

        // restore striker/nonStriker/bowler on MatchState
        if (!cb.getEventType().equals("wicket")) {
            m.setStriker(batsman.getPlayer());
            m.setNonStriker(nonStriker.getPlayer());
            m.setBowler(bowler.getPlayer());
        }

        // safe rate calculations — avoid division by zero
        int totalBalls = (m.getOvers() * 6) + m.getBalls();
        if (totalBalls > 0) {
            m.setRr((double) m.getRuns() / totalBalls);
            m.setCrr((double) m.getRuns() * 6 / totalBalls);
        } else {
            m.setRr(0.0);
            m.setCrr(0.0);
        }

        // target adjustment

            // second innings: target is what's needed, so ADD back

            if (totalBalls > 0) {
                m.setRequiredRR((double) m.getTarget() * 6 / totalBalls);
            } else {
                m.setRequiredRR(0.0);
            }



        // delete the ball first, then save everything
        cricketBallInterface.delete(cb);
        matchStateInterface.save(m);
        playerInningsInterface.save(nonStriker);
        playerInningsInterface.save(batsman);
        playerInningsInterface.save(bowler);

        ScoreDTO s = convertToScoreDTO(m, false, false, "");



        return s;
    }



    private void handleUndoWicket(MatchState m, PlayerInnings batsman, PlayerInnings bowler, PlayerInnings nonStriker, CricketBall cb) {
        switch (cb.getDismissalType().toLowerCase()) {
            case "runout":
            case "bowled":
            case "caught":
            case "hitwicket":
            case "stumped":
            case "lbw":
            case "overthefence":
            case "onehandonebounce":
                if (m.getBalls() == 0) m.setLastOverBowlerId(null);
                decrementBall(m);
                bowler.setWickets(bowler.getWickets() - 1);
                bowler.setBallsBowled(bowler.getBallsBowled() - 1);
            case "retired":
            case "mankad":
                m.setWickets(m.getWickets() - 1);
                if (cb.getBatsman().getId().equals(cb.getOutPlayer().getId())) {
                    m.setStriker(cb.getOutPlayer());
                    m.setNonStriker(cb.getNonStriker());
                } else {
                    m.setStriker(cb.getBatsman());
                    m.setNonStriker(cb.getOutPlayer());
                }
                if (cb.getDismissalType().equalsIgnoreCase("runout"))
                    batsman.setRuns(batsman.getRuns() - cb.getRuns());
                batsman.setBallsFaced(batsman.getBallsFaced());
                batsman.setRr((double) batsman.getRuns() / batsman.getBallsFaced());
                if (cb.getRuns() == 4) batsman.setFour(batsman.getFour() - 1);
                if (cb.getRuns() == 6) batsman.setSixes(batsman.getSixes() - 1);
                bowler.setRunsConceded(bowler.getRunsConceded() - cb.getRuns());

                bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());
                m.setRuns(m.getRuns() - cb.getRuns());

        }
        Player undoneOutPlayer = cb.getOutPlayer();
        if (undoneOutPlayer != null) {
            PlayerInnings outPI = playerInningsInterface
                    .findByInnings_IdAndPlayer_Id(
                            cb.getInnings().getId(), undoneOutPlayer.getId());
            if (outPI != null) {
                outPI.setDismissed(false);
                playerInningsInterface.save(outPI);
            }
        }
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CachePut(value = "matchStates", key = "#result.matchId")
    public Object scoring(JsonNode rawPayload) {
        // ✅ JsonNode -> ScoreDTO — ObjectMapper safely convert karta hai
        ScoreDTO score = objectMapper.convertValue(rawPayload, ScoreDTO.class);
        return scoreCricket(score);
    }



    // ─────────────────────────────────────────
    // INTERNAL — original logic (unchanged)
    // ─────────────────────────────────────────

    private ScoreDTO scoreCricket(ScoreDTO score) {

        // ─── DLS: no ball, no players needed — just target update ────
        if ("dls".equalsIgnoreCase(score.getEventType())) {
            MatchState m = matchStateInterface.findByInnings_Id(score.getInningsId());
            m.setTarget(score.getDlsTarget());
            int tb = m.getOvers() * 6 + m.getBalls();
            if (tb > 0) m.setRequiredRR((double) m.getTarget() * 6 / tb);
            matchStateInterface.save(m);
            ScoreDTO result = convertToScoreDTO(m, false, false, "");
            result.setComment("DLS_UPDATED");
            return result;
        }
        // ─────────────────────────────────────────────────────────────

        Player batsmanPlayer    = playerInterface.findActiveById(score.getBatsmanId()).get();
        Player bowlerPlayer     = playerInterface.findActiveById(score.getBowlerId()).get();
        Player nonStrikerPlayer = playerInterface.findActiveById(score.getNonStrikerId()).get();
        Player outPlayer   = score.getOutPlayerId() != null
                ? playerInterface.findActiveById(score.getOutPlayerId()).get() : null;
        Player newPlayer   = score.getNewPlayerId() != null
                ? playerInterface.findActiveById(score.getNewPlayerId()).get() : null;
        Player fielderPlayer = score.getFielderId() != null
                ? playerInterface.findActiveById(score.getFielderId()).get() : null;

        Match match        = matchInterface.findById(score.getMatchId()).get();
        CricketInnings ci  = cricketInningsInterface.findById(score.getInningsId()).get();
        MatchState m       = matchStateInterface.findByInnings_Id(score.getInningsId());

        PlayerInnings batsman    = playerInningsInterface.findByInnings_IdAndPlayer_Id(score.getInningsId(), score.getBatsmanId());
        PlayerInnings bowler     = playerInningsInterface.findByInnings_IdAndPlayer_Id(score.getInningsId(), score.getBowlerId());
        PlayerInnings nonStriker = playerInningsInterface.findByInnings_IdAndPlayer_Id(score.getInningsId(), score.getNonStrikerId());

        if (m == null) m = new MatchState();
        if (batsman == null)    batsman = new PlayerInnings();
        if (bowler == null)     bowler = new PlayerInnings();
        if (nonStriker == null) nonStriker = new PlayerInnings();
        if (m.getStatus() == null) m.setStatus("LIVE");

        if (batsman.getPlayer() == null)    batsman.setPlayer(batsmanPlayer);
        if (bowler.getPlayer() == null)     bowler.setPlayer(bowlerPlayer);
        if (nonStriker.getPlayer() == null) nonStriker.setPlayer(nonStrikerPlayer);

        batsman.setInnings(ci);
        bowler.setInnings(ci);
        nonStriker.setInnings(ci);

        if (score.getDismissalType() != null) {
            score.setDismissalType(score.getDismissalType().replace(" ", ""));
        }

        m.setBowler(bowlerPlayer);
        if (outPlayer == null) {
            m.setStriker(batsmanPlayer);
            m.setNonStriker(nonStrikerPlayer);
        } else {
            if (Objects.equals(score.getOutPlayerId(), score.getBatsmanId())) {
                m.setStriker(newPlayer);
                m.setNonStriker(nonStrikerPlayer);
            } else {
                m.setStriker(batsmanPlayer);
                m.setNonStriker(newPlayer);
            }
            PlayerInnings existingNew = playerInningsInterface
                    .findByInnings_IdAndPlayer_Id(score.getInningsId(), newPlayer.getId());
            if (existingNew == null) {
                PlayerInnings newPI = new PlayerInnings();
                newPI.setPlayer(newPlayer);
                newPI.setInnings(ci);
                playerInningsInterface.save(newPI);
            }
        }

        BallContext ctx = new BallContext(batsmanPlayer, bowlerPlayer, nonStrikerPlayer,
                outPlayer, fielderPlayer, match, ci);

        CricketBall cricketBall = new CricketBall();
        m.setInnings(ci);

        m = processEvent(score, m, cricketBall, batsman, bowler, ctx);

        boolean isWideOrNoBall = "wide".equalsIgnoreCase(score.getEventType())
                || "noball".equalsIgnoreCase(score.getEventType());
        if (!isWideOrNoBall && m.getBalls() == 0 && m.getOvers() > 0) {
            m.setLastOverBowlerId(bowlerPlayer.getId());
        }

// ── Playing bench: mark dismissed player ─────────────────────────
// When a wicket falls, mark the out player's PlayerInnings as dismissed.
// convertToScoreDTO will then exclude them from availableBatters.
        if (outPlayer != null && "wicket".equalsIgnoreCase(score.getEventType())) {
            PlayerInnings outPI = playerInningsInterface
                    .findByInnings_IdAndPlayer_Id(score.getInningsId(), outPlayer.getId());
            if (outPI == null) {
                outPI = new PlayerInnings();
                outPI.setPlayer(outPlayer);
                outPI.setInnings(ci);
            }
            outPI.setDismissed(true);
            playerInningsInterface.save(outPI);
        }
        //
        // Innings + Super Over both skip ball saving ────────────
        boolean isEndInnings = "End_Innings".equals(score.getEventType())
                || "Super_Over".equals(score.getEventType());
        // ── Penalty doesn't rotate strikers ──────────────────────────
        boolean isPenalty = "penalty".equalsIgnoreCase(score.getEventType());

        if (isEndInnings) {
            score.setComment("");
            score.setEventType("");
        }

        boolean shouldRotate = false;
        if (!isEndInnings) {
            cricketBall.setInnings(ci);
            if (!isPenalty) bowler.setRole("BOWLER");
            cricketBallInterface.save(cricketBall);
            matchStateInterface.save(m);
            playerInningsInterface.save(nonStriker);
            playerInningsInterface.save(batsman);
            playerInningsInterface.save(bowler);
            if (!isPenalty) {
                shouldRotate = checkRotate(Integer.parseInt(score.getEvent()));
            }
        }

        return convertToScoreDTO(m, shouldRotate, isEndInnings, "");
    }
    private MatchState processEvent(ScoreDTO score, MatchState m, CricketBall c,
                                    PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        boolean a = false;

        switch (score.getEventType()) {

            case "run":
            case "boundary":
                addScore(score, m, c, batsman, bowler, ctx);
                break;

            case "wide":
            case "noball":
            case "legbye":
            case "bye":
                handleExtras(score, m, c, batsman, bowler, ctx);
                break;

            case "wicket":
                handleWickets(score, m, c, batsman, bowler, ctx);
                break;

            // ── PENALTY ───────────────────────────────────────────────
            case "penalty": {
                int pr = Integer.parseInt(score.getEvent());
                m.setRuns(m.getRuns() + pr);
                m.setExtras(m.getExtras() + pr);
                if (score.isFirstInnings()) {
                    m.setTarget(m.getTarget() + pr);
                } else {
                    m.setTarget(m.getTarget() - pr);
                    int tb = m.getOvers() * 6 + m.getBalls();
                    if (tb > 0) m.setRequiredRR((double) m.getTarget() * 6 / tb);
                }
                int tb = m.getOvers() * 6 + m.getBalls();
                m.setCrr(tb > 0 ? (double) m.getRuns() * 6 / tb : 0);

                c.setRuns(pr);
                c.setEventType("penalty");
                c.setEvent(score.getEvent());
                c.setLegalDelivery(false);
                c.setBatsman(ctx.batsman);
                c.setBowler(ctx.bowler);
                c.setNonStriker(ctx.nonStriker);
                c.setMatch(ctx.match);
                c.setBallNumber(m.getBalls());
                c.setOverNumber(m.getOvers());
                break;
            }

            // ── END INNINGS ───────────────────────────────────────────
            case "End_Innings":
                a = true;
                if (score.isFirstInnings() && !score.isSuperOver()) {
                    // ── Regular 1st innings ended → start 2nd innings ─
                    score.setFirstInnings(false);
                    Match match = ctx.match;

                    CricketInnings firstInnings = match.getCricketInnings().get(0);
                    Team firstBattingTeam = firstInnings.getTeam();
                    Team secondBattingTeam = firstBattingTeam.getId().equals(match.getTeam1().getId())
                            ? match.getTeam2() : match.getTeam1();

                    CricketInnings newInnings = new CricketInnings();
                    newInnings.setNo(2);
                    newInnings.setMatch(match);
                    newInnings.setTeam(secondBattingTeam);
                    newInnings = cricketInningsInterface.save(newInnings);

                    MatchState newState = new MatchState();
                    newState.setInnings(newInnings);
                    newState.setRuns(0);
                    newState.setWickets(0);
                    newState.setRr(0.0);
                    newState.setCrr(0.0);
                    newState.setExtras(0);
                    newState.setOvers(0);
                    newState.setBalls(0);
                    newState.setTarget(score.getTarget()+1);
                    newState.setStatus("LIVE");
                    m = matchStateInterface.save(newState);
                    score.setComment("");

                } else if (score.isFirstInnings() && score.isSuperOver()) {
                    // ── Super Over 1st innings ended → start SO 2nd innings ─
                    Match soMatch = ctx.match;
                    Team firstSOTeam = m.getInnings().getTeam();
                    Team secondSOTeam = firstSOTeam.getId().equals(soMatch.getTeam1().getId())
                            ? soMatch.getTeam2() : soMatch.getTeam1();

                    CricketInnings soInnings2 = new CricketInnings();
                    soInnings2.setNo(soMatch.getCricketInnings().size() + 1);
                    soInnings2.setMatch(soMatch);
                    soInnings2.setSuper_Over(true);
                    soInnings2.setTeam(secondSOTeam);
                    soInnings2 = cricketInningsInterface.save(soInnings2);

                    MatchState soState2 = new MatchState();
                    soState2.setInnings(soInnings2);
                    soState2.setRuns(0);
                    soState2.setWickets(0);
                    soState2.setTarget(score.getTarget()); // SO 1st innings total+1
                    soState2.setOvers(0);
                    soState2.setBalls(0);
                    soState2.setRr(0.0);
                    soState2.setCrr(0.0);
                    soState2.setExtras(0);
                    soState2.setStatus("LIVE");
                    soState2.setTarget(score.getRuns()+1);
                    m = matchStateInterface.save(soState2);
                    score.setComment("");

                } else {

                    Match match = ctx.match;
                    Team winnerTeam;
                    List<CricketInnings> ci=matchInterface.findteambyinningsandmatch(match.getId());
                    System.out.println(ci.get(0).getTeam().getName() + " vs " + ci.get(1).getTeam().getName());
                    if (score.getTarget() <= 0) {

                        if(score.isSuperOver())
                            winnerTeam = ci.get(0).getTeam();
                        else
                            winnerTeam = ci.get(1).getTeam();

                    } else {
                        // first batting team won
                        if(score.isSuperOver())
                            winnerTeam = ci.get(1).getTeam();
                        else
                            winnerTeam = ci.get(0).getTeam();
                    }
                    match.setWinnerTeam(winnerTeam);
                    score.setComment(winnerTeam.getName());
                    score.setMatchEnd(true);
                    m.setMatchEnd(true);
                    matchInterface.save(match);
                    matchService.endMatch(match.getId());
                }
                break;

            // ── SUPER OVER ────────────────────────────────────────────
            case "Super_Over":
                a = true;
                Match soMatch = ctx.match;

                if (!score.isSuperOver()) {
                    // ── First ever super over triggered (tie at end of 2nd innings) ─
                    CricketInnings soInnings1 = new CricketInnings();
                    soInnings1.setNo(soMatch.getCricketInnings().size() + 1); // 3
                    soInnings1.setMatch(soMatch);
                    soInnings1.setSuper_Over(true);
                    soInnings1.setTeam(m.getInnings().getTeam()); // 2nd innings team bats first in SO
                    soInnings1 = cricketInningsInterface.save(soInnings1);

                    MatchState soState = new MatchState();
                    soState.setInnings(soInnings1);
                    soState.setRuns(0);
                    soState.setWickets(0);
                    soState.setTarget(0);
                    soState.setOvers(0);
                    soState.setBalls(0);
                    soState.setRr(0.0);
                    soState.setCrr(0.0);
                    soState.setExtras(0);
                    soState.setStatus("LIVE");
                    m = matchStateInterface.save(soState);

                    score.setInningsId(soInnings1.getId());
                    score.setSuperOver(true);
                    score.setFirstInnings(true);
                    score.setComment("");

                } else {
                    // ── Another super over (SO 2nd innings also tied) ─
                    // Find last SO 1st innings team
                    int size = soMatch.getCricketInnings().size();
                    Team prevFirstSOTeam = soMatch.getCricketInnings().get(size - 2).getTeam();

                    // Same team that batted 2nd in last SO bats first in new SO
                    CricketInnings newSO = new CricketInnings();
                    newSO.setNo(size + 1);
                    newSO.setMatch(soMatch);
                    newSO.setSuper_Over(true);
                    newSO.setTeam(m.getInnings().getTeam()); // current (losing) 2nd SO team bats first
                    newSO = cricketInningsInterface.save(newSO);

                    MatchState newSOState = new MatchState();
                    newSOState.setInnings(newSO);
                    newSOState.setRuns(0);
                    newSOState.setWickets(0);
                    newSOState.setTarget(0);
                    newSOState.setOvers(0);
                    newSOState.setBalls(0);
                    newSOState.setRr(0.0);
                    newSOState.setCrr(0.0);
                    newSOState.setExtras(0);
                    newSOState.setStatus("LIVE");
                    m = matchStateInterface.save(newSOState);

                    score.setInningsId(newSO.getId());
                    score.setSuperOver(true);
                    score.setFirstInnings(true);
                    score.setComment("");
                }
                break;

            // ── SO 2nd innings end → End_Innings is sent with isSuperOver=true, firstInnings=false
            // Already handled above in End_Innings case. No separate case needed.
        }

        c.setEvent(score.getEvent());
        c.setEventType(score.getEventType());
        return m;
    }

    private void handleWickets(ScoreDTO score, MatchState m, CricketBall c,
                               PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        String d = score.getDismissalType().toLowerCase().trim();
        switch (d) {
            case "bowled":
            case "caught":
            case "hitwicket":
            case "stumped":
            case "lbw":
            case "overthefence":
            case "onehandonebounce":
            case "runout":
            case "retired":
            case "mankad":
                handleNormalWickets(score, m, c, batsman, bowler, ctx);
                break;
        }
    }

    private void handleNormalWickets(ScoreDTO score, MatchState m, CricketBall c,
                                     PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        String r = score.getDismissalType().toLowerCase();
        c.setDismissalType(score.getDismissalType());
        c.setBatsman(batsman.getPlayer());
        c.setNonStriker(ctx.nonStriker);
        c.setOutPlayer(ctx.outPlayer);  // ✅ No DB call
        c.setBowler(bowler.getPlayer());
        if (ctx.fielder != null)
            c.setFielder(ctx.fielder);  // ✅ No DB call
        c.setRuns(score.getRunsOnThisBall());
        c.setLegalDelivery(true);
        if (!r.equals("retired") && !r.equals("mankad")) {
            incrementBall(m);
            bowler.setBallsBowled(bowler.getBallsBowled() + 1);
            bowler.setWickets(bowler.getWickets() + 1);

        }
        if (score.getDismissalType().equalsIgnoreCase("runout")) {
            batsman.setRuns(batsman.getRuns() + score.getRunsOnThisBall());
        }
        c.setBallNumber(m.getBalls());
        c.setOverNumber(m.getOvers());
        c.setMatch(ctx.match);       // ✅ No DB call
        c.setInnings(ctx.innings);   // ✅ No DB call
        c.setEventType("wicket");

        m.setWickets(m.getWickets() + 1);
        if (m.getWickets() == 10) {
            m.setStatus("WICKET-OUT");
        }
        m.setRuns(m.getRuns() + score.getRunsOnThisBall());
        if (score.isFirstInnings()) {
            m.setTarget(m.getTarget() + score.getRunsOnThisBall());
        } else {
            m.setTarget(m.getTarget() - score.getRunsOnThisBall());
            m.setRequiredRR((double) m.getTarget() * 6 / ((m.getOvers() * 6) + m.getBalls()));
        }
        bowler.setRunsConceded(bowler.getRunsConceded() + score.getRunsOnThisBall());


        m.setCrr((double) m.getRuns() * 6 / ((m.getOvers() * 6) + m.getBalls()));

    }

    private void addScore(ScoreDTO score, MatchState m, CricketBall c,
                          PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        int r = Integer.parseInt(score.getEvent());
        m.setRuns(m.getRuns() + r);

        if (score.isFirstInnings()) {
            m.setTarget(m.getTarget() + r);
        }else{
            m.setTarget(m.getTarget() - r);
        }
        incrementBall(m);

        c.setNonStriker(ctx.nonStriker);  // ✅ No DB call
        c.setRuns(r);
        c.setBatsman(ctx.batsman);        // ✅ No DB call
        c.setBowler(ctx.bowler);          // ✅ No DB call
        c.setLegalDelivery(true);
        c.setBallNumber(m.getBalls());
        c.setOverNumber(m.getOvers());
        c.setMatch(ctx.match);            // ✅ No DB call

        bowler.setRunsConceded(bowler.getRunsConceded() + r);
        bowler.setBallsBowled(bowler.getBallsBowled() + 1);
        bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());

        batsman.setRuns(batsman.getRuns() + r);
        batsman.setBallsFaced(batsman.getBallsFaced() + 1);
        batsman.setRr((double) batsman.getRuns() / batsman.getBallsFaced());

        m.setCrr((double) m.getRuns() * 6 / ((m.getOvers() * 6) + m.getBalls()));
        if (r == 4) {
            c.setIsFour(true);
            batsman.setFour(batsman.getFour() + 1);
        } else if (r == 6) {
            c.setIsSix(true);
            batsman.setSixes(batsman.getSixes() + 1);
        }


    }

    private void handleExtras(ScoreDTO score, MatchState m, CricketBall c,
                              PlayerInnings batsman, PlayerInnings bowler, BallContext ctx) {
        int r = Integer.parseInt(score.getEvent());




        if (!score.getEventType().equalsIgnoreCase("wide") && !score.getEventType().equalsIgnoreCase("noball")) {
            bowler.setBallsBowled(bowler.getBallsBowled() + 1);
            bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());
            batsman.setBallsFaced(batsman.getBallsFaced() + 1);
            batsman.setRr((double) batsman.getRuns() / batsman.getBallsFaced());
            m.setExtras(m.getExtras() + r);
            incrementBall(m);
            c.setExtraType(score.getEventType());
            c.setLegalDelivery(true);
            m.setRuns(m.getRuns() + r);
            if (score.isFirstInnings()) {
                m.setTarget(m.getTarget() + r);
            } else {
                m.setTarget(m.getTarget() - r);
                m.setRequiredRR((double) m.getTarget() * 6 / ((m.getOvers() * 6) + m.getBalls()));
            }
        } else {
            bowler.setRunsConceded(bowler.getRunsConceded() + r + 1);
            bowler.setEco((double) bowler.getRunsConceded() / bowler.getBallsBowled());

            if (score.getEventType().equalsIgnoreCase("wide")) {
                m.setExtras(m.getExtras() + r + 1);
                m.setRuns(m.getRuns() + r + 1);
                c.setExtraType("wide");
                if (score.isFirstInnings()) {
                    m.setTarget(m.getTarget() + r+1);

                }
                else {
                    m.setTarget(m.getTarget() - r-1);
                    m.setRequiredRR((double) m.getTarget() * 6 / ((m.getOvers() * 6) + m.getBalls()));
                }
            } else {
                m.setExtras(m.getExtras() + 1);
                m.setRuns(m.getRuns() + r + 1);
                batsman.setRuns(batsman.getRuns() + r);
                batsman.setBallsFaced(batsman.getBallsFaced() + 1);
                batsman.setRr((double) batsman.getRuns() / batsman.getBallsFaced());
                if (r == 4) batsman.setFour(batsman.getFour() + 1);
                else if (r == 6) batsman.setSixes(batsman.getSixes() + 1);
                c.setExtraType("noball");
                c.setExtra(1);
                if (score.isFirstInnings()) {
                    m.setTarget(m.getTarget() + r+1);

                }
                else {
                    m.setTarget(m.getTarget() - r-1);
                    m.setRequiredRR((double) m.getTarget() * 6 / ((m.getOvers() * 6) + m.getBalls()));
                }
            }
            c.setLegalDelivery(false);
        }

        if (r == 4) c.setIsFour(true);
        else if (r == 6) c.setIsSix(true);
        c.setRuns(r);
        c.setNonStriker(ctx.nonStriker);  // ✅ No DB call
        c.setBatsman(ctx.batsman);        // ✅ No DB call
        c.setBowler(ctx.bowler);          // ✅ No DB call
        c.setMatch(ctx.match);            // ✅ No DB call
        c.setBallNumber(m.getBalls());
        c.setOverNumber(m.getOvers());

        m.setCrr((double) m.getRuns() * 6 / ((m.getOvers() * 6) + m.getBalls()));

    }

    void incrementBall(MatchState m) {
        if (m.getBalls() + 1 >= 6) {
            m.setOvers(m.getOvers() + 1);
            m.setBalls(0);
        } else {
            m.setBalls(m.getBalls() + 1);
        }
    }

    void decrementBall(MatchState m) {
        if (m.getBalls() == 0) {
            m.setOvers(m.getOvers() - 1);
            m.setBalls(5);
        } else {
            m.setBalls(m.getBalls() - 1);
        }
    }

    private boolean checkRotate(int r) {
        return r % 2 != 0;
    }


    // ------- Inner class to hold pre-fetched entities -------
    private static class BallContext {
        final Player batsman;
        final Player bowler;
        final Player nonStriker;
        final Player outPlayer;
        final Player fielder;
        final Match match;
        final CricketInnings innings;

        BallContext(Player batsman, Player bowler, Player nonStriker,
                    Player outPlayer, Player fielder, Match match, CricketInnings innings) {
            this.batsman = batsman;
            this.bowler = bowler;
            this.nonStriker = nonStriker;
            this.outPlayer = outPlayer;
            this.fielder = fielder;
            this.match = match;
            this.innings = innings;
        }
    }

}