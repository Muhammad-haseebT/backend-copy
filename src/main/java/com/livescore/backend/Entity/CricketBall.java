package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class CricketBall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "innings_id")
    @JsonBackReference("innings-balls")
    private CricketInnings innings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    @JsonBackReference("match-balls")
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batsman_id")
    @JsonIgnore
    private Player batsman;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nonStriker_id")
    @JsonIgnore
    private Player nonStriker;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bowler_id")
    @JsonIgnore
    private Player bowler;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fielder_id")
    @JsonIgnore
    private Player fielder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "out_batsman_id")
    @JsonIgnore
    private Player outBatsman;

    private Integer overNumber;
    private Integer ballNumber;

    private Integer runs;
    private Integer extra;
    private String extraType;
    private String dismissalType;
    private  String eventType;
    private String event;

    private Boolean legalDelivery = true;
    private Boolean isFour = false;
    private Boolean isSix = false;

    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "out_player_id")
    @JsonIgnore
    public Player outPlayer;

    @OneToMany(mappedBy = "ball", cascade = CascadeType.ALL)
    @JsonManagedReference("ball-media")
    @JsonIgnore
    private List<Media> mediaList = new ArrayList<>();

    @Column(name = "is_super_over",nullable = true)
    boolean super_over = false;



}
