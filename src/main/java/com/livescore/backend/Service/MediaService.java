//package com.livescore.backend.Service;
//
//import com.livescore.backend.DTO.MediaDTO;
//import com.livescore.backend.Entity.Match;
//import com.livescore.backend.Entity.Media;
//import com.livescore.backend.Interface.MatchInterface;
//import com.livescore.backend.Interface.MediaInterface;
//import com.livescore.backend.Interface.TournamentInterface;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.*;
//
//@Service
//
//public class MediaService {
//    @Autowired
//    private MediaInterface mediaInterface;
//    @Autowired
//    private MatchInterface matchInterface;
//    @Autowired
//    private TournamentInterface tournamentInterface;
//    private String path = "E:\\FYP\\Backend\\media";
//
//    public ResponseEntity<?> createMedia(MultipartFile f, MediaDTO media) throws IOException {
//
//        if (f == null || f.isEmpty()) {
//            return ResponseEntity.badRequest().body(
//                    Map.of("error", "Media file is required")
//            );
//        }
//
//        if (media == null) {
//            return ResponseEntity.badRequest().body(
//                    Map.of("error", "Media details are required")
//            );
//        }
//
//
//        if (media.getMatchId() == null) {
//            return ResponseEntity.badRequest().body(
//                    Map.of("error", "Match ID is required")
//            );
//        }
//
//        // Check if match exists
//        Optional<Match> matchOpt = matchInterface.findById(media.getMatchId());
//        if (matchOpt.isEmpty()) {
//            return ResponseEntity.badRequest().body(
//                    Map.of("error", "Match not found with ID: " + media.getMatchId())
//            );
//        }
//        //save file in folder
//        String fileName = f.getOriginalFilename();
//        if (fileName == null || fileName.isBlank()) {
//            return ResponseEntity.badRequest().body(
//                    Map.of("error", "Invalid file name")
//            );
//        }
//        String filePath = path + fileName;
//        File file = new File(filePath);
//        //if file already exist than do nothing
//        if(file.exists()){
//            return ResponseEntity.badRequest().body(
//                    Map.of("error", "File already exists")
//            );
//        }
//        f.transferTo(file);
//
//
//        Media mediaEntity = new Media();
//        mediaEntity.setFileUrl(filePath);
//        mediaEntity.setFileType(f.getContentType());
//        mediaEntity.setMatch(matchOpt.get());
//
//        // Save and return
//        Media savedMedia = mediaInterface.save(mediaEntity);
//
//        return ResponseEntity.ok(Map.of(
//                "message", "Media uploaded successfully",
//                "mediaId", savedMedia.getId(),
//                "fileType", savedMedia.getFileType(),
//                "matchId", savedMedia.getMatch().getId()
//        ));
//    }
//
//    public ResponseEntity<?> getMediaById(Long id) {
//        return mediaInterface.findById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    public ResponseEntity<?> getAllMedia() {
//        return ResponseEntity.ok(mediaInterface.findAll());
//    }
//
//
//
//    public ResponseEntity<?> deleteMedia(Long id) {
//        if(mediaInterface.existsById(id)){
//            mediaInterface.deleteById(id);
//            return ResponseEntity.ok().build();
//        }
//        return ResponseEntity.notFound().build();
//    }
//
//    private String getFileExtension(String filename) {
//        if (filename == null || !filename.contains(".")) return "unknown";
//        return filename.substring(filename.lastIndexOf('.') + 1);
//    }
//
//    public ResponseEntity<?> getMediaByMatchId(Long matchId) {
//    List<Media> mediaList = mediaInterface.findByMatchId(matchId);
//
//    List<Map<String, Object>> response = new ArrayList<>();
//
//    for (Media m : mediaList) {
//
//        try {
//            // Read file from disk
//            Path filePath = Paths.get(m.getFileUrl()); // <-- Your saved path field
//            byte[] fileBytes = Files.readAllBytes(filePath);
//
//            Map<String, Object> mediaMap = new HashMap<>();
//            mediaMap.put("id", m.getId());
//            mediaMap.put("fileType", m.getFileType());
//            mediaMap.put("fileName", filePath.getFileName().toString());
//            mediaMap.put("data", Base64.getEncoder().encodeToString(fileBytes));
//
//            response.add(mediaMap);
//
//        } catch (IOException e) {
//            return ResponseEntity.status(500).body("Failed to read file: " + m.getFileUrl());
//        }
//    }
//
//    return ResponseEntity.ok(response);
//}
//
//
//    public ResponseEntity<?> getMediaBySeasonId(Long id) {
//        List<Media> mediaList = mediaInterface.findMediaBySeasonId(id);
//        if(mediaList.stream().count()==0){
//            return ResponseEntity.notFound().build();
//        }
//       return ResponseEntity.ok(mediaList);
//
//
//
//    }
//}



package com.livescore.backend.Service;

import com.livescore.backend.DTO.MediaDTO;
import com.livescore.backend.Entity.CricketBall;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Entity.Media;
import com.livescore.backend.Interface.CricketBallInterface;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.MediaInterface;
import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.exceptions.*;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class MediaService {

    @Autowired
    private MediaInterface mediaInterface;

    @Autowired
    private MatchInterface matchInterface;

    @Autowired
    private ImageKit imageKit;

    @Value("local") // local ya imagekit
    private String uploadMode;


    private String localPath="C:\\Users\\mht34\\Desktop\\server images";
    @Autowired
    private CricketBallInterface cricketBallInterface;

    @CacheEvict(value = "matchStates",key="#media.matchId")
    public ResponseEntity<?> createMedia(MultipartFile f, MediaDTO media) throws IOException, ForbiddenException, TooManyRequestsException, InternalServerException, UnauthorizedException, BadRequestException, UnknownException {

        // Validation checks (aapke existing checks)
        if (f == null || f.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Media file is required")
            );
        }

        if (media.getMatchId() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Match ID is required")
            );
        }

        Optional<Match> matchOpt = matchInterface.findById(media.getMatchId());
        if (matchOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Match not found")
            );
        }

        String fileUrl;

        // Mode ke basis par upload karo
        if ("imagekit".equalsIgnoreCase(uploadMode)) {
            fileUrl = uploadToImageKit(f);
        } else {
            fileUrl = uploadToLocal(f);
        }

        // Database mein save karo
        Media mediaEntity = new Media();
        mediaEntity.setFileUrl(fileUrl);
        mediaEntity.setFileType(f.getContentType());
        mediaEntity.setMatch(matchOpt.get());
        mediaEntity.setComment(media.getComment()); // ✅ NEW field set karo
        CricketBall b=cricketBallInterface.findById(media.getBallId()).orElse(null);
        mediaEntity.setBall(b);
        Media savedMedia = mediaInterface.save(mediaEntity);
        assert b != null;
        b.setMediaList(mediaInterface.findByBallId(b.getId()));
        cricketBallInterface.save(b);
        return ResponseEntity.ok(Map.of(
                "message", "Media uploaded successfully",
                "mediaId", savedMedia.getId(),
                "fileUrl", fileUrl,
                "uploadMode", uploadMode
        ));
    }

    // ImageKit upload helper
    private String uploadToImageKit(MultipartFile f) throws IOException, ForbiddenException, TooManyRequestsException, InternalServerException, UnauthorizedException, BadRequestException, UnknownException {
        byte[] fileBytes = f.getBytes();
        String base64 = Base64.getEncoder().encodeToString(fileBytes);

        FileCreateRequest uploadRequest = new FileCreateRequest(
                base64,
                f.getOriginalFilename()
        );
        uploadRequest.setFolder("/livescore-media/");

        Result uploadResult = imageKit.upload(uploadRequest);
        return uploadResult.getUrl(); // Public URL milega
    }

    // Local upload helper (aapka existing code)
    private String uploadToLocal(MultipartFile f) throws IOException {
        String fileName = f.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IOException("Invalid file name");
        }

        File dir = new File(localPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create upload directory: " + localPath);
        }

        String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        File file = new File(dir, safeName);
        if (file.exists()) {
            String unique = System.currentTimeMillis() + "_" + UUID.randomUUID();
            file = new File(dir, unique + "_" + safeName);
        }

        f.transferTo(file);
        return file.getAbsolutePath();
    }


    public ResponseEntity<?> getMediaById(Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "id is required")
            );
        }
        return mediaInterface.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    public ResponseEntity<?> getMediaByMatchId(Long matchId) {
        List<Media> mediaList = mediaInterface.findByMatchId(matchId);
        List<Map<String, Object>> response = new ArrayList<>();

        for (Media m : mediaList) {
            Map<String, Object> mediaMap = new HashMap<>();
            mediaMap.put("id", m.getId());
            mediaMap.put("fileType", m.getFileType());

            // Agar ImageKit URL hai to directly send karo
            if (m.getFileUrl().startsWith("http")) {
                mediaMap.put("url", m.getFileUrl());
                mediaMap.put("mode", "imagekit");
            } else {
                // Local file ko base64 mein convert karo
                try {
                    Path filePath = Paths.get(m.getFileUrl());
                    byte[] fileBytes = Files.readAllBytes(filePath);
                    mediaMap.put("data", Base64.getEncoder().encodeToString(fileBytes));
                    mediaMap.put("mode", "local");
                } catch (IOException e) {
                    continue;
                }
            }

            response.add(mediaMap);
        }

        return ResponseEntity.ok(response);
    }
    //get all media
    public ResponseEntity<?> getAllMedia() {
        List<Media> m= mediaInterface.findAll();
        if (m == null || m.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mediaToDto(m));
    }

    public ResponseEntity<?> getMediaBySeasonId(Long id, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Media> mediaList = mediaInterface.findMediaBySeasonId(id, pageable);

        if (mediaList == null || mediaList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(mediaToDto(mediaList));
    }

    List<Map<String, Object>> mediaToDto(List<Media> mediaList) {
        List<Map<String, Object>> responses = new ArrayList<>();

        for (Media media : mediaList) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", media.getId());
            response.put("fileType", media.getFileType());
            response.put("mode", "local");

            try {
                File file = new File(media.getFileUrl());
                if (file.exists()) {

                    byte[] fileContent = Files.readAllBytes(file.toPath());


                    String base64Image = Base64.getEncoder().encodeToString(fileContent);


                    String dataUrl = "data:" + media.getFileType() + ";base64," + base64Image;

                    response.put("url", dataUrl);
                } else {
                    response.put("url", null);
                    response.put("error", "File not found on server");
                }
            } catch (IOException e) {
                response.put("url", null);
                response.put("error", "Error reading file: " + e.getMessage());
            }

            responses.add(response);
        }
        return responses;
    }

    public ResponseEntity<?> getMediaByTournamentId(Long id,int page,int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Media> mediaList = mediaInterface.findMediaByTournamentId(id,pageable);
        if (mediaList == null || mediaList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mediaToDto(mediaList));
    }

    public ResponseEntity<?> getMediaBySportId(Long id,int page,int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Media> mediaList = mediaInterface.findMediaBySportId(id,pageable);
        if (mediaList == null || mediaList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mediaToDto(mediaList));
    }


}

