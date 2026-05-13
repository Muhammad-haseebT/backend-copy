package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Sports;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportsInterface extends JpaRepository<Sports, Long> {
    boolean existsByName(String name);
}
