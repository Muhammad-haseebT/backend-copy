package com.livescore.backend.Config;

import com.livescore.backend.Interface.multisportgeneric.ScoringServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ScoringServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(ScoringServiceFactory.class);

    // Spring automatically injects all ScoringServiceInterface beans
    // key = @Service("CRICKET") / @Service("FUTSAL") etc.
    private final Map<String, ScoringServiceInterface> services;

    public ScoringServiceFactory(Map<String, ScoringServiceInterface> services) {
        this.services = services;
        log.info("ScoringServiceFactory initialized with sports: {}", services.keySet());
    }

    /**
     * Sport name se service lo.
     * sportName case-insensitive hai — "futsal", "FUTSAL", "Futsal" sab work karenge.
     */
    public ScoringServiceInterface getService(String sportName) {
        if (sportName == null || sportName.isBlank()) {
            throw new IllegalArgumentException("Sport name cannot be null or blank");
        }

        String key = sportName.toUpperCase().trim();
        ScoringServiceInterface service = services.get(key);

        if (service == null) {
            log.error("No scoring service found for sport='{}'. Available sports: {}",
                    key, services.keySet());
            throw new IllegalArgumentException(
                    "No scoring service for sport: " + key +
                            ". Available: " + services.keySet());
        }

        return service;
    }
}