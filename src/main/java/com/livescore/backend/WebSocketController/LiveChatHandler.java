package com.livescore.backend.WebSocketController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LiveChatHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LiveChatHandler.class);
    private final ObjectMapper mapper = new ObjectMapper();

    // matchId -> connected sessions
    private final Map<Long, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // sessionId -> matchId (one session = one room)
    private final Map<String, Long> sessionRoom = new ConcurrentHashMap<>();

    // sessionId -> username
    private final Map<String, String> sessionUsername = new ConcurrentHashMap<>();

    // sessionId -> send lock
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────
    // Connection lifecycle
    // ─────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            closeQuietly(session);
            return;
        }

        Long matchId = null;
        String username = "Anonymous";

        for (String param : uri.getQuery().split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length < 2) continue;
            if ("matchId".equals(kv[0])) {
                try { matchId = Long.parseLong(kv[1]); }
                catch (NumberFormatException e) { log.warn("Invalid matchId: {}", kv[1]); }
            }
            if ("username".equals(kv[0])) {
                username = kv[1].trim().isEmpty() ? "Anonymous" : kv[1].trim();
            }
        }

        if (matchId == null) {
            safeSend(session, errorJson("matchId is required in query params"));
            closeQuietly(session);
            return;
        }

        // Register session
        sessionLocks.put(session.getId(), new Object());
        sessionRoom.put(session.getId(), matchId);
        sessionUsername.put(session.getId(), username);
        rooms.computeIfAbsent(matchId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);

        log.info("Chat connected: session={} matchId={} username={}", session.getId(), matchId, username);

        // Notify room that user joined
        broadcastToRoom(matchId, systemMessage(username + " joined the chat"), session.getId());

        // Confirm to sender
        safeSend(session, systemMessage("Connected to chat for match " + matchId));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long matchId = sessionRoom.remove(session.getId());
        String username = sessionUsername.remove(session.getId());
        sessionLocks.remove(session.getId());

        if (matchId != null) {
            Set<WebSocketSession> room = rooms.get(matchId);
            if (room != null) {
                room.remove(session);
                if (room.isEmpty()) rooms.remove(matchId);
            }
            if (username != null) {
                broadcastToRoom(matchId, systemMessage(username + " left the chat"), null);
            }
        }
        log.info("Chat disconnected: session={}", session.getId());
    }

    // ─────────────────────────────────────────────
    // Message handling
    // ─────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = mapper.readTree(message.getPayload());

            String text = node.has("message") ? node.get("message").asText("").trim() : "";
            if (text.isEmpty()) {
                safeSend(session, errorJson("message field is required and cannot be empty"));
                return;
            }

            // Optional: override username mid-session
            if (node.has("username") && !node.get("username").asText().isBlank()) {
                sessionUsername.put(session.getId(), node.get("username").asText().trim());
            }

            String username = sessionUsername.getOrDefault(session.getId(), "Anonymous");
            Long matchId = sessionRoom.get(session.getId());

            if (matchId == null) {
                safeSend(session, errorJson("Session not associated with any match"));
                return;
            }

            String outgoing = chatMessage(username, text, matchId);
            broadcastToRoom(matchId, outgoing, null); // null = broadcast to everyone including sender

        } catch (Exception e) {
            log.error("Error handling chat message from session={}", session.getId(), e);
            safeSend(session, errorJson("Invalid JSON payload"));
        }
    }

    // ─────────────────────────────────────────────
    // Broadcast
    // ─────────────────────────────────────────────

    /**
     * @param excludeSessionId  null = send to all, non-null = skip that session
     */
    private void broadcastToRoom(Long matchId, String json, String excludeSessionId) {
        Set<WebSocketSession> room = rooms.get(matchId);
        if (room == null || room.isEmpty()) return;

        List<WebSocketSession> dead = new ArrayList<>();

        for (WebSocketSession s : new ArrayList<>(room)) {
            if (!s.isOpen()) { dead.add(s); continue; }
            if (s.getId().equals(excludeSessionId)) continue;
            safeSend(s, json);
        }

        dead.forEach(this::removeDeadSession);
    }

    private void removeDeadSession(WebSocketSession session) {
        Long matchId = sessionRoom.remove(session.getId());
        sessionUsername.remove(session.getId());
        sessionLocks.remove(session.getId());
        if (matchId != null) {
            Set<WebSocketSession> room = rooms.get(matchId);
            if (room != null) {
                room.remove(session);
                if (room.isEmpty()) rooms.remove(matchId);
            }
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
                if (session.isOpen()) session.sendMessage(new TextMessage(payload));
            } catch (Exception e) {
                log.warn("Send failed for session={}", session.getId(), e);
            }
        }
    }

    // ─────────────────────────────────────────────
    // JSON builders
    // ─────────────────────────────────────────────

    private String chatMessage(String username, String text, Long matchId) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("type", "chat");
            node.put("matchId", matchId);
            node.put("username", username);
            node.put("message", text);
            node.put("timestamp", Instant.now().toString());
            return mapper.writeValueAsString(node);
        } catch (Exception e) { return "{}"; }
    }

    private String systemMessage(String text) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("type", "system");
            node.put("message", text);
            node.put("timestamp", Instant.now().toString());
            return mapper.writeValueAsString(node);
        } catch (Exception e) { return "{}"; }
    }

    private String errorJson(String msg) {
        return "{\"type\":\"error\",\"message\":\"" + msg + "\"}";
    }

    private void closeQuietly(WebSocketSession session) {
        try { session.close(); } catch (Exception ignored) {}
    }
}
