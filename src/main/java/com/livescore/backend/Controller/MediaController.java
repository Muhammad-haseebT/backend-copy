package com.livescore.backend.Controller;

import com.livescore.backend.DTO.MediaDTO;
import com.livescore.backend.Entity.Media;
import com.livescore.backend.Service.MediaService;
import io.imagekit.sdk.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
@RestController

public class MediaController {
    @Autowired
    private MediaService mediaService;

    //10 mb limit
    @PostMapping(value = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMedia(
            @RequestPart("file") MultipartFile file,
            @RequestParam("matchId") Long matchId,
            @RequestParam("ballId") Long ballId,
            @RequestParam(value = "comment", required = false) String comment // ✅ NEW
    ) throws IOException, ForbiddenException, TooManyRequestsException,
            InternalServerException, UnauthorizedException, BadRequestException, UnknownException {

        MediaDTO media = new MediaDTO();
        media.setMatchId(matchId);
        media.setBallId(ballId);
        media.setComment(comment); // ✅ NEW
        return mediaService.createMedia(file, media);
    }

    @GetMapping("/media/{id}")
    public ResponseEntity<?> getMediaById(@PathVariable Long id) {
        return mediaService.getMediaById(id);
    }

    @GetMapping("/media")
    public ResponseEntity<?> getAllMedia() {
        return mediaService.getAllMedia();
    }

//
//    @DeleteMapping("/media/{id}")
//    public ResponseEntity<?> deleteMedia(@PathVariable Long id) {
//        return mediaService.deleteMedia(id);
//    }
//
//    //get media by match id(send files from path to api caller)
//    @GetMapping("media/match/{id}")
//    public ResponseEntity<?> getMediaByMatchId(@PathVariable Long id) {
//        return mediaService.getMediaByMatchId(id);
//    }
    @GetMapping("media/season/{id}/{page}/{size}")
    public ResponseEntity<?> getMediaBySeasonId(@PathVariable Long id, @PathVariable int page, @PathVariable int size) {
        return mediaService.getMediaBySeasonId(id, page, size);
    }
    @GetMapping("media/tournament/{id}/{page}/{size}")
    public ResponseEntity<?> getMediaByTournamentId(@PathVariable Long id, @PathVariable int page, @PathVariable int size) {
        return mediaService.getMediaByTournamentId(id, page, size);
    }
    //get by sport id
    @GetMapping("media/sport/{id}/{page}/{size}")
    public ResponseEntity<?> getMediaBySportId(@PathVariable Long id, @PathVariable int page, @PathVariable int size) {
        return mediaService.getMediaBySportId(id, page, size);
    }








}
