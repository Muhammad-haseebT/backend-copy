package com.livescore.backend.Service;

import com.livescore.backend.Entity.*;
import com.livescore.backend.Interface.Cricket.MatchStateInterface;
import com.livescore.backend.Interface.Cricket.PlayerInningsInterface;
import com.livescore.backend.Interface.CricketBallInterface;
import com.livescore.backend.Interface.FavouritePlayerInterface;
import com.livescore.backend.Entity.Player;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.PlayerInterface;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerInningsService {
    private final PlayerInningsInterface playerInningsInterface;
    private final MatchInterface matchInterface;
    private final MatchStateInterface matchStateInterface;
    private final CricketBallInterface cricketBallInterface;
    private final FavouritePlayerInterface favouritePlayerInterface;
    private final PlayerInterface playerInterface;



    public ResponseEntity<?> getScorecard(Long matchId, Long Team1Id) {
        Match m = matchInterface.findById(matchId).orElse(null);

        if (m == null) {
            return ResponseEntity.notFound().build();
        }
        MatchState matchState = matchStateInterface
                .findAll().stream()
                .filter(ms -> ms.getInnings().getMatch().getId().equals(matchId)
                        && ms.getInnings().getTeam().getId().equals(Team1Id)
                        && !ms.getInnings().isSuper_Over())
                .findFirst().orElse(null);

        List<PlayerInnings> playerInningsList1 = playerInningsInterface.findByMatchIdAndTeamId(matchId, Team1Id).stream()
                .filter(pi -> !pi.getInnings().isSuper_Over())
                .toList();;
        Scorecard scorecard = new Scorecard();
        List<batsmanScore> batsmanScores = new ArrayList<>();
        List<bowlerScore> bowlerScores = new ArrayList<>();
        for (PlayerInnings playerInnings : playerInningsList1) {

            if(Objects.equals(playerInnings.getRole(), "Batsman")){
                batsmanScore bs = new batsmanScore();
                bs.setName(playerInnings.getPlayer().getName());
                bs.setRuns(playerInnings.getRuns());
                bs.setBalls(playerInnings.getBallsFaced());
                bs.setFours(playerInnings.getFour());
                bs.setSixes(playerInnings.getSixes());
                bs.setStrikeRate((double) playerInnings.getRuns() / (playerInnings.getBallsFaced() == 0 ? 1 : playerInnings.getBallsFaced()) * 100);
                batsmanScores.add(bs);
            }


            if(Objects.equals(playerInnings.getRole(), "BOWLER")){
                System.out.println("Bowler: " + playerInnings.getPlayer().getName() + ", Runs: " + playerInnings.getRuns() + ", Balls: " + playerInnings.getBallsFaced() + ", Wickets: " + playerInnings.getWickets());
                 bowlerScore bl = new bowlerScore();
                bl.setName(playerInnings.getPlayer().getName());
                bl.setOvers(playerInnings.getOvers() );
                bl.setEconomy((double) playerInnings.getRunsConceded() / (playerInnings.getBallsBowled() == 0 ? 1 : playerInnings.getBallsBowled()) * 6);
                bl.setRunsConceded(playerInnings.getRunsConceded());
                bl.setWickets(playerInnings.getWickets());
                bl.setBallsBowled(playerInnings.getBallsBowled());
                bowlerScores.add(bl);

            }

        }
        scorecard.setBatsmanScores(batsmanScores);
        scorecard.setBowlerScores(bowlerScores);
        if (matchState != null) {
            scorecard.setExtras(matchState.getExtras());
            scorecard.setTotalRuns(matchState.getRuns());
            scorecard.setOvers(matchState.getOvers());
            scorecard.setBalls(matchState.getBalls());
        } else {
            scorecard.setExtras(0);
            scorecard.setTotalRuns(0);
            scorecard.setOvers(0);
            scorecard.setBalls(0);
        }
        return ResponseEntity.ok(scorecard);

    }

    public ResponseEntity<?> getSummary(Long mid) {
        List<PlayerInnings> p1=playerInningsInterface.findByMatchIdAndInninsNo(mid,1);
        List<PlayerInnings> p2=playerInningsInterface.findByMatchIdAndInninsNo(mid,2);
        if (p1.isEmpty() || p2.isEmpty()) return ResponseEntity.badRequest().body("Innings data missing");
        MatchState m1=matchStateInterface.findByMatchIdAndInningsNo(mid,1);
        MatchState m2=matchStateInterface.findByMatchIdAndInningsNo(mid,2);


        summary summary = new summary();
        Match m = matchInterface.findById(mid).orElse(null);
        String result = m.getWinnerTeam() != null ? m.getWinnerTeam().getName() + " won" : "Match Tied";
        summary.setResult(result);
        String mom = m.getManOfMatch() != null ? m.getManOfMatch().getName() : "TBD";
        summary.setManOfTheMatch(mom);

        summary.setTeam1Name(p1.get(0).getInnings().getTeam().getName());
        summary.setTeam2Name(p2.get(0).getInnings().getTeam().getName());

        summary.setTeam1Runs(m1.getRuns());
        summary.setTeam2Runs(m2.getRuns());
        summary.setTeam1Overs(m1.getOvers());
        summary.setTeam2Overs(m2.getOvers());
        summary.setTeam1Wickets(m1.getWickets());
        summary.setTeam2Wickets(m2.getWickets());

        List<PlayerPerformanceDTO> topBatsmen1 = new ArrayList<>();
        List<PlayerPerformanceDTO> topBowlers1 = new ArrayList<>();
        List<PlayerPerformanceDTO> topBowlers2 = new ArrayList<>();
        List<PlayerPerformanceDTO> topBatsmen2 = new ArrayList<>();


        List<PlayerInnings> topBatsman1 = p1.stream().filter(pi -> pi.getRole().equals("Batsman")).sorted((pi1, pi2) -> Integer.compare(pi2.getRuns(), pi1.getRuns())).limit(3).toList();
        List<PlayerInnings> topBatsman2 = p2.stream().filter(pi -> pi.getRole().equals("Batsman")).sorted((pi1, pi2) -> Integer.compare(pi2.getRuns(), pi1.getRuns())).limit(3).toList();
        List<PlayerInnings> topBowler1 = p1.stream().filter(pi -> pi.getRole().equals("BOWLER")).sorted((pi1, pi2) -> Integer.compare(pi2.getWickets(), pi1.getWickets())).limit(3).toList();
        List<PlayerInnings> topBowler2 = p2.stream().filter(pi -> pi.getRole().equals("BOWLER")).sorted((pi1, pi2) -> Integer.compare(pi2.getWickets(), pi1.getWickets())).limit(3).toList();

        for (PlayerInnings pi : topBatsman1) {
            PlayerPerformanceDTO dto = new PlayerPerformanceDTO();
            dto.setPlayerName(pi.getPlayer().getName());
            dto.setRuns(pi.getRuns());
            dto.setOversFaced((double)(pi.getOvers()*6+pi.getBallsFaced())/6);

            topBatsmen1.add(dto);
        }

        for (PlayerInnings pi : topBatsman2) {
            PlayerPerformanceDTO dto = new PlayerPerformanceDTO();
            dto.setPlayerName(pi.getPlayer().getName());
            dto.setRuns(pi.getRuns());
            dto.setOversFaced((double)(pi.getOvers()*6+pi.getBallsFaced())/6);
            topBatsmen2.add(dto);
        }
        for (PlayerInnings pi : topBowler1) {
            PlayerPerformanceDTO dto = new PlayerPerformanceDTO();
            dto.setPlayerName(pi.getPlayer().getName());
            dto.setRunsConceded(pi.getRunsConceded());
            dto.setWickets(pi.getWickets());
            dto.setOversBowled((double)(pi.getOvers()*6+pi.getBallsBowled())/6);
            topBowlers1.add(dto);
        }

        for (PlayerInnings pi : topBowler2) {
            PlayerPerformanceDTO dto = new PlayerPerformanceDTO();
            dto.setPlayerName(pi.getPlayer().getName());
            dto.setRunsConceded(pi.getRunsConceded());
            dto.setWickets(pi.getWickets());
            dto.setOversBowled((double)(pi.getOvers()*6+pi.getBallsBowled())/6);
            topBowlers2.add(dto);
        }


        summary.setTopBatsmen1(topBatsmen1);
        summary.setTopBatsmen2(topBatsmen2);
        summary.setTopBowlers1(topBowlers1);
        summary.setTopBowlers2(topBowlers2);


        return ResponseEntity.ok(summary);

    }
    public ResponseEntity<List<BallTabDto>> getMatchBalls(Long mid, Long tid) {
        List<CricketBall> balls = cricketBallInterface.findByMatchId(mid, tid);

        List<BallTabDto> dtoList = balls.stream().map(ball -> {

            // 1. Tag Logic (1wd, W, 4, 6 etc)
            String displayEvent = ball.getEvent() != null ? ball.getEvent() : "0";
            if (ball.getDismissalType() != null && !ball.getDismissalType().isEmpty()) {
                displayEvent = "W";
            } else if ("extra".equalsIgnoreCase(ball.getEventType()) && ball.getExtraType() != null) {
                String type = ball.getExtraType().toLowerCase();
                if (type.contains("wide")) displayEvent = ball.getExtra() + "wd";
                else if (type.contains("no")) displayEvent = ball.getExtra() + "nb";
                else if (type.contains("leg")) displayEvent = ball.getExtra() + "lb";
                else if (type.contains("bye")) displayEvent = ball.getExtra() + "b";
            }

            Integer runs = ball.getRuns() != null ? ball.getRuns() : 0;
            Integer extra = ball.getExtra() != null ? ball.getExtra() : 0;

            // 2. Mapping to DTO (Arguments order MUST match DTO class)
            return new BallTabDto(
                    ball.getId(),                                       // 1. id
                    ball.getOverNumber() + "." + ball.getBallNumber(), // 2. overBall
                    ball.getBatsman() != null ? ball.getBatsman().getName() : "Unknown", // 3. batsmanName
                    ball.getBowler() != null ? ball.getBowler().getName() : "Unknown",   // 4. bowlerName
                    ball.getNonStriker() != null ? ball.getNonStriker().getName() : "",  // 5. nonStrikerName
                    ball.getFielder() != null ? ball.getFielder().getName() : null,      // 6. fielderName
                    ball.getOutPlayer() != null ? ball.getOutPlayer().getName() : null,  // 7. outPlayerName
                    runs,                                               // 8. runs
                    extra,                                              // 9. extra
                    ball.getExtraType(),                                // 10. extraType
                    displayEvent,                                       // 11. event (display)
                    ball.getEventType(),                                // 12. eventType
                    ball.getDismissalType(),                            // 13. dismissalType
                    ball.getComment(),                                  // 14. comment
                    (ball.getDismissalType() != null),                  // 15. isWicket
                    (Boolean.TRUE.equals(ball.getIsFour()) || Boolean.TRUE.equals(ball.getIsSix())) // 16. isBoundary
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }



    @Data
    @AllArgsConstructor
    @NoArgsConstructor // Safety ke liye
    public class BallTabDto {
        private Long id;
        private String overBall;
        private String batsmanName;
        private String bowlerName;
        private String nonStrikerName;
        private String fielderName;
        private String outPlayerName;
        private Integer runs;
        private Integer extra;
        private String extraType;
        private String event;         // Ye wahi "displayEvent" hai (e.g., "1wd", "W", "6")
        private String eventType;     // "run", "boundary", etc.
        private String dismissalType;
        private String comment;
        private Boolean isWicket;     // Boolean check for frontend colors
        private Boolean isBoundary;   // Boolean check for frontend colors
    }

    @Data
    static class summary{
        private String team1Name;
        private String team2Name;

        private int team1Runs;
        private int team2Runs;

        private int team1Wickets;
        private int team2Wickets;

        private double team1Overs;
        private double team2Overs;

        private List<PlayerPerformanceDTO> topBatsmen1;
        private List<PlayerPerformanceDTO> topBowlers1;

        private List<PlayerPerformanceDTO> topBatsmen2;
        private List<PlayerPerformanceDTO> topBowlers2;

        private String manOfTheMatch;
        private String result;
    }

    @Data
    public static class PlayerPerformanceDTO {
        private String playerName;
        private int runs;
        private int runsConceded;
        private int wickets;
        private double oversFaced;
        private double oversBowled;

    }

    @Data
    static class Scorecard {

        private List<batsmanScore> batsmanScores = null;
        private List<bowlerScore> bowlerScores = null;
        private int Extras = 0;
        private int TotalRuns = 0;
        private int overs = 0;
        private int balls = 0;


    }

    @Data
    static class batsmanScore {
        private String name = "";
        private int runs = 0;
        private int balls = 0;
        private int fours = 0;
        private int sixes = 0;
        private double strikeRate = 0.0;
    }

    @Data
    static class bowlerScore {
        private String name = "";
        private int overs = 0;
        private double economy = 0.0;
        private int runsConceded = 0;
        private int wickets = 0;
        private int ballsBowled = 0;
    }

}
