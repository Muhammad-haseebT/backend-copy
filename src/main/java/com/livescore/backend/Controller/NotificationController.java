package com.livescore.backend.Controller;

import com.livescore.backend.Service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")

public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccountNotifications(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationsByAccountId(id));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        boolean updated = notificationService.markAsRead(id);
        if (updated) {
            return ResponseEntity.ok("Notification marked as read");
        }
        return ResponseEntity.notFound().build();
    }
}
