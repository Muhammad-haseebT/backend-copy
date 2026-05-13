package com.livescore.backend.Controller;

import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Interface.CricketBallInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CricketBallController {
    @Autowired
    private CricketBallInterface cricketBallInterface;
    @GetMapping("/cricketBall")
    public List<CricketBall> getAllCricketBall() {
        return cricketBallInterface.findAll();
    }
//  innings id ni ye innings no hai
    @GetMapping("/cricketBall/{over}/{balls}/{matchID}/{inningsId}")
    public List<CricketBall> getCricketBallById(@PathVariable Integer over, @PathVariable Integer balls, @PathVariable Long matchID,@PathVariable int inningsId) {

        return cricketBallInterface.findByOverNumberAndBallNumberAndMatch_Id(over, balls, matchID,inningsId);
    }
}
