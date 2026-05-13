package com.livescore.backend.WebSocketController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livescore.backend.Entity.Match;
import com.livescore.backend.Interface.MatchInterface;
import com.livescore.backend.Interface.multisportgeneric.ScoringServiceInterface;
import com.livescore.backend.Config.ScoringServiceFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

@Component
public class LiveScoringHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveScoringHandler.class);

    private final ScoringServiceFactory scoringServiceFactory;
    private final MatchInterface matchInterface;
    private final ObjectMapper mapper = new ObjectMapper();

    // matchId -> subscribed sessions
    private final Map<Long, Set<WebSocketSession>> subscriptions = new ConcurrentHashMap<>();

    // sessionId -> subscribed matchIds
    private final Map<String, Set<Long>> sessionSubscriptions = new ConcurrentHashMap<>();

    // sessionId -> lock object (safe concurrent sends ke liye)
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public LiveScoringHandler(ScoringServiceFactory scoringServiceFactory,
                              MatchInterface matchInterface) {
        this.scoringServiceFactory = scoringServiceFactory;
        this.matchInterface = matchInterface;
    }

    // ─────────────────────────────────────────────
    // Shutdown
    // ─────────────────────────────────────────────

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                log.warn("Executor did not terminate gracefully, forced shutdown");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ─────────────────────────────────────────────
    // Connection lifecycle
    // ─────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) return;

        for (String q : uri.getQuery().split("&")) {
            if (q.startsWith("matchId=")) {
                try {
                    Long matchId = Long.parseLong(q.split("=")[1]);
                    subscribe(session, matchId);
                } catch (NumberFormatException e) {
                    log.warn("Invalid matchId in WebSocket URI: {}", uri.getQuery());
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.debug("WebSocket closed: session={} status={}", session.getId(), status);
        unsubscribeAll(session);
    }

    // ─────────────────────────────────────────────
    // Subscription management
    // ─────────────────────────────────────────────

    private void subscribe(WebSocketSession session, Long matchId) {
        if (matchId == null) return;

        subscriptions
                .computeIfAbsent(matchId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);

        sessionSubscriptions
                .computeIfAbsent(session.getId(), k -> ConcurrentHashMap.newKeySet())
                .add(matchId);

        sessionLocks.computeIfAbsent(session.getId(), k -> new Object());

        executor.execute(() -> sendCurrentMatchState(session, matchId));
    }

    private void unsubscribeAll(WebSocketSession session) {
        Set<Long> matches = sessionSubscriptions.remove(session.getId());
        if (matches != null) {
            for (Long matchId : matches) {
                Set<WebSocketSession> set = subscriptions.get(matchId);
                if (set != null) {
                    set.remove(session);
                    if (set.isEmpty()) subscriptions.remove(matchId);
                }
            }
        }
        sessionLocks.remove(session.getId());
    }

    // ─────────────────────────────────────────────
    // Message handling
    // ─────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = mapper.readTree(message.getPayload());

            // ── SUBSCRIBE action ──────────────────────────────────────────
            if (node.has("action")
                    && "subscribe".equalsIgnoreCase(node.get("action").asText())
                    && node.has("matchId")) {

                Long matchId = node.get("matchId").asLong();
                subscribe(session, matchId);
                safeSend(session, "{\"status\":\"subscribed\",\"matchId\":" + matchId + "}");
                return;
            }

            // ── matchId mandatory ─────────────────────────────────────────
            if (!node.has("matchId") || node.get("matchId").isNull()) {
                safeSend(session, "{\"error\":\"matchId is required\"}");
                return;
            }

            Long matchId = node.get("matchId").asLong();

            // ── UNDO action ───────────────────────────────────────────────
            boolean isUndo = node.has("undo") && node.get("undo").asBoolean(false);
            if (isUndo) {
                // inningsId optional — futsal mein null bhi chalega
                Long inningsId = node.has("inningsId") && !node.get("inningsId").isNull()
                        ? node.get("inningsId").asLong()
                        : null;
                executor.execute(() -> processUndo(matchId, inningsId));
                return;
            }

            // ── SCORE action — raw JsonNode service ko pass karo ─────────
            executor.execute(() -> processScore(matchId, node));

        } catch (Exception e) {
            log.error("Error handling WebSocket message from session={}", session.getId(), e);
            safeSend(session, "{\"error\":\"Invalid JSON payload\"}");
        }
    }

    // ─────────────────────────────────────────────
    // Core processing
    // ─────────────────────────────────────────────

    private void sendCurrentMatchState(WebSocketSession session, Long matchId) {
        try {
            ScoringServiceInterface service = getServiceForMatch(matchId);
            if (service == null) return;

            Object state = service.getCurrentMatchState(matchId);
            safeSend(session, mapper.writeValueAsString(state));
        } catch (Exception e) {
            log.warn("Failed to send current match state for matchId={}", matchId, e);
        }
    }

    /**
     * Raw JsonNode service ko pass karo.
     * Har service apna DTO khud convert karegi — Cricket ScoreDTO, Futsal FutsalScoreDTO.
     */
    private void processScore(Long matchId, JsonNode rawPayload) {
        try {
            ScoringServiceInterface service = getServiceForMatch(matchId);
            if (service == null) return;

            Object updated = service.scoring(rawPayload);
            broadcastObject(updated, matchId);
        } catch (Exception e) {
            log.warn("Failed to process score for matchId={}", matchId, e);
        }
    }

    private void processUndo(Long matchId, Long inningsId) {
        try {
            ScoringServiceInterface service = getServiceForMatch(matchId);
            if (service == null) return;

            Object updated = service.undoLastBall(matchId, inningsId);
            broadcastObject(updated, matchId);
        } catch (Exception e) {
            log.warn("Failed to process undo for matchId={}", matchId, e);
        }
    }

    // ─────────────────────────────────────────────
    // Factory helper
    // ─────────────────────────────────────────────

    private ScoringServiceInterface getServiceForMatch(Long matchId) {
        try {
            Match match = matchInterface.findByIdWithSport(matchId)
                    .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));

            String sportName = resolveSportName(match);
            return scoringServiceFactory.getService(sportName);

        } catch (Exception e) {
            log.error("Could not resolve sport service for matchId={}", matchId, e);
            return null;
        }
    }

    /**
     * Match entity se sport name nikalo.
     * Tournament -> Sport -> getName() — e.g. "futsal", "cricket"
     * Factory mein toUpperCase() hota hai, toh yahan kuch bhi return karo.
     */
    private String resolveSportName(Match match) {
        if (match.getTournament() != null
                && match.getTournament().getSport() != null) {
            return match.getTournament().getSport().getName();
        }
        throw new RuntimeException("Sport not found on match id=" + match.getId());
    }

    // ─────────────────────────────────────────────
    // Broadcast
    // ─────────────────────────────────────────────

    /**
     * Kisi bhi Object ko broadcast karo — ScoreDTO ya FutsalScoreDTO dono chalenge.
     * matchId alag pass karte hain kyunki Object pe getMatchId() nahi hota.
     */
    private void broadcastObject(Object payload, Long matchId) {
        if (payload == null || matchId == null) return;

        Set<WebSocketSession> subs = subscriptions.get(matchId);
        if (subs == null || subs.isEmpty()) return;

        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize payload for broadcast, matchId={}", matchId, e);
            return;
        }

        List<WebSocketSession> snapshot = new ArrayList<>(subs);
        List<WebSocketSession> deadSessions = new ArrayList<>();

        for (WebSocketSession s : snapshot) {
            if (!s.isOpen()) {
                deadSessions.add(s);
                continue;
            }
            try {
                safeSend(s, json);
            } catch (Exception ex) {
                log.warn("Broadcast failed for session={}", s.getId());
                deadSessions.add(s);
            }
        }

        for (WebSocketSession dead : deadSessions) {
            unsubscribeAll(dead);
            try { dead.close(); } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────
    // Safe send
    // ─────────────────────────────────────────────

    private void safeSend(WebSocketSession session, String payload) {
        if (session == null || !session.isOpen()) return;

        Object lock = sessionLocks.computeIfAbsent(session.getId(), k -> new Object());

        synchronized (lock) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (Exception e) {
                log.warn("Error sending to session={}", session.getId(), e);
                // synchronized block ke bahar — deadlock avoid karne ke liye
                executor.execute(() -> unsubscribeAll(session));
            }
        }
    }
}