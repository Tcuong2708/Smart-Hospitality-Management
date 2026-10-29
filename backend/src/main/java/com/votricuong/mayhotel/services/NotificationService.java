package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.Notification;
import com.votricuong.mayhotel.repositories.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final SequenceGeneratorService sequenceGenerator;

    public NotificationService(NotificationRepository repository, SequenceGeneratorService sequenceGenerator) {
        this.repository = repository;
        this.sequenceGenerator = sequenceGenerator;
    }

    public Notification create(Long customerId, Long userId, String type, String title,
                               String message, String icon, String link) {
        return repository.save(Notification.builder()
                .id(sequenceGenerator.generateSequence("notifications_sequence"))
                .customerId(customerId).userId(userId).type(type).title(title).message(message)
                .icon(icon).link(link).isRead(false).createdAt(new Date()).build());
    }

    public List<Notification> getByCustomer(Long customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public long countUnread(Long customerId) {
        return repository.countByCustomerIdAndIsReadFalse(customerId);
    }

    public boolean markAsRead(Long notificationId, Long customerId) {
        return repository.findByIdAndCustomerId(notificationId, customerId).map(item -> {
            if (!Boolean.TRUE.equals(item.getIsRead())) {
                item.setIsRead(true);
                item.setReadAt(new Date());
                repository.save(item);
            }
            return true;
        }).orElse(false);
    }

    public void markAllAsRead(Long customerId) {
        List<Notification> unread = repository.findByCustomerIdAndIsReadFalse(customerId);
        Date now = new Date();
        unread.forEach(item -> {
            item.setIsRead(true);
            item.setReadAt(now);
        });
        repository.saveAll(unread);
    }
}
