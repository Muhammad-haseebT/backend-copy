package com.livescore.backend.Interface;

import com.livescore.backend.Entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByAccount_IdOrderByCreatedAtDesc(Long accountId);
}
