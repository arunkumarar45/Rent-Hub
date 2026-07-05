package com.renthub.notification.service;

import com.renthub.notification.model.entity.Notification;

import java.util.List;

public interface NotificationService {
    List<Notification> getNotificationsForUser(Long userId);
    long getUnreadCount(Long userId);
    void markAsRead(Long notificationId);
    void createInAppNotification(Long userId, String title, String message);
    
    // Email Operations
    void sendEmail(String toEmail, String subject, String body);
    void sendBookingConfirmationEmail(String toEmail, String customerName, String itemTitle, String dates, double price);
    void sendBookingCancellationEmail(String toEmail, String customerName, String itemTitle, String reason);
    void sendBookingReminderEmail(String toEmail, String customerName, String itemTitle, String dates);
    void sendOtpEmail(String toEmail, String otp);
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
