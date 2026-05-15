package com.livescore.backend.Service;

import com.livescore.backend.DTO.NotificationDTO;
import com.livescore.backend.Entity.Account;
import com.livescore.backend.Entity.Notification;
import com.livescore.backend.Entity.NotificationType;
import com.livescore.backend.Interface.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public void createNotification(Account account, String title, String message, NotificationType type) {
        if (account == null) return;
        Notification notification = new Notification();
        notification.setAccount(account);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public List<NotificationDTO> getNotificationsByAccountId(Long accountId) {
        return notificationRepository.findByAccount_IdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public boolean markAsRead(Long notificationId) {
        return notificationRepository.findById(notificationId).map(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
            return true;
        }).orElse(false);
    }

    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setAccountId(notification.getAccount().getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
