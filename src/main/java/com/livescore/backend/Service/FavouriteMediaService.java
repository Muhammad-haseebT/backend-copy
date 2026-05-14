package com.livescore.backend.Service;

import com.livescore.backend.Entity.Account;
import com.livescore.backend.Entity.FavouriteMedia;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Media;
import com.livescore.backend.Interface.AccountInterface;
import com.livescore.backend.Interface.FavouriteMediaInterface;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.MediaInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavouriteMediaService {

    @Autowired
    private FavouriteMediaInterface favouriteMediaInterface;

    @Autowired
    private AccountInterface accountInterface;

    @Autowired
    private MediaInterface mediaInterface;

    @Autowired
    private MatchInterface matchInterface;

    @Autowired
    private MediaService mediaService; // to reuse mediaToDto if needed, or we just construct the response

    public ResponseEntity<?> toggleFavourite(Long accountId, Long mediaId, Long matchId) {
        Optional<Account> accountOpt = accountInterface.findById(accountId);
        Optional<Media> mediaOpt = mediaInterface.findById(mediaId);
        Optional<Match> matchOpt = matchInterface.findById(matchId);

        if (accountOpt.isEmpty() || mediaOpt.isEmpty() || matchOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid account, media, or match ID"));
        }

        Optional<FavouriteMedia> existing = favouriteMediaInterface.findByAccountIdAndMediaId(accountId, mediaId);

        if (existing.isPresent()) {
            favouriteMediaInterface.delete(existing.get());
            return ResponseEntity.ok(Map.of("isFavourite", false));
        } else {
            FavouriteMedia fav = new FavouriteMedia();
            fav.setAccount(accountOpt.get());
            fav.setMedia(mediaOpt.get());
            fav.setMatch(matchOpt.get());
            favouriteMediaInterface.save(fav);
            return ResponseEntity.ok(Map.of("isFavourite", true));
        }
    }

    public ResponseEntity<?> getMyFavourites(Long accountId) {
        List<FavouriteMedia> favs = favouriteMediaInterface.findByAccountId(accountId);
        List<Media> mediaList = favs.stream().map(FavouriteMedia::getMedia).collect(Collectors.toList());
        return ResponseEntity.ok(mediaService.mediaToDto(mediaList)); // reusing mediaService helper
    }

    public ResponseEntity<?> getMatchFavourites(Long matchId, Long accountId) {
        List<FavouriteMedia> favs = favouriteMediaInterface.findByMatchIdAndAccountId(matchId, accountId);
        List<Long> mediaIds = favs.stream().map(f -> f.getMedia().getId()).collect(Collectors.toList());
        return ResponseEntity.ok(mediaIds);
    }
}
