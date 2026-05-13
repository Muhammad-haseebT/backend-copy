package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
// Season.java
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {
        "account",
        "tournaments"
})
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private LocalDate createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDate.now();
    }


    @ManyToOne
    @JoinColumn(name = "account_id")

    private Account account;


    @ManyToMany
    @JoinTable(name = "season_sports",
            joinColumns = @JoinColumn(name = "season_id"),
            inverseJoinColumns = @JoinColumn(name = "sports_id"))
    private List<Sports> sportsOffered = new ArrayList<>();

    // Season -> Tournament (one-to-many)
    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Tournament> tournaments = new ArrayList<>();


}
