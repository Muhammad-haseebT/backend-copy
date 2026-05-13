package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
@Entity
@Data
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference("match-media")
    private Match match;

    private String fileUrl;

    @Column(nullable = false)
    private String fileType;

    @ManyToOne
    @JoinColumn(name = "ball_id")
    @JsonBackReference("ball-media")
    private CricketBall ball;

    // ✅ NEW
    @Column(length = 500)
    private String comment;
}
