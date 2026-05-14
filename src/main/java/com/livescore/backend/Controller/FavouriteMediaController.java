package com.livescore.backend.Controller;

import com.livescore.backend.Service.FavouriteMediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/media/favourite")
public class FavouriteMediaController {

    @Autowired
    private FavouriteMediaService favouriteMediaService;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleFavourite(@RequestBody Map<String, Long> payload) {
        Long accountId = payload.get("accountId");
        Long mediaId = payload.get("mediaId");
        Long matchId = payload.get("matchId");
        
        if (accountId == null || mediaId == null || matchId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "accountId, mediaId, and matchId are required"));
        }
        
        return favouriteMediaService.toggleFavourite(accountId, mediaId, matchId);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getMyFavourites(@PathVariable Long accountId) {
        return favouriteMediaService.getMyFavourites(accountId);
    }

    @GetMapping("/match/{matchId}/account/{accountId}")
    public ResponseEntity<?> getMatchFavourites(@PathVariable Long matchId, @PathVariable Long accountId) {
        return favouriteMediaService.getMatchFavourites(matchId, accountId);
    }
}
