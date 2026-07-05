package com.renthub.notification.service;

import com.renthub.auth.model.entity.User;
import com.renthub.auth.repository.UserRepository;
import com.renthub.common.exception.ResourceNotFoundException;
import com.renthub.notification.model.entity.Notification;
import com.renthub.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void createInAppNotification(Long userId, String title, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
        log.info("Saved in-app notification for user ID: {}", userId);
    }

    @Override
    public void sendEmail(String toEmail, String subject, String body) {
        log.info("[EMAIL SENT] To: {} | Subject: {} | Content: {}", toEmail, subject, body);
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Email logged to console.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Successfully sent email to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}", toEmail, e);
        }
    }

    @Override
    public void sendBookingConfirmationEmail(String toEmail, String customerName, String itemTitle, String dates, double price) {
        String subject = "Booking Confirmed: " + itemTitle;
        String body = String.format("Hi %s,\n\nYour booking for '%s' for the dates %s has been confirmed.\nTotal Price: $%.2f (inclusive of security deposit).\n\nThank you for choosing RentHub!",
                customerName, itemTitle, dates, price);
        sendEmail(toEmail, subject, body);
    }

    @Override
    public void sendBookingCancellationEmail(String toEmail, String customerName, String itemTitle, String reason) {
        String subject = "Booking Cancelled: " + itemTitle;
        String body = String.format("Hi %s,\n\nYour booking for '%s' has been cancelled.\nReason: %s.\nAny deposits paid will be refunded according to our cancellation policy.\n\nBest regards,\nRentHub Support",
                customerName, itemTitle, reason);
        sendEmail(toEmail, subject, body);
    }

    @Override
    public void sendBookingReminderEmail(String toEmail, String customerName, String itemTitle, String dates) {
        String subject = "Rental Reminder: " + itemTitle;
        String body = String.format("Hi %s,\n\nThis is a friendly reminder that your rental period for '%s' starts on %s.\nPlease coordinate with the owner for pick-up arrangements.\n\nEnjoy your rental!\nRentHub Team",
                customerName, itemTitle, dates);
        sendEmail(toEmail, subject, body);
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        String subject = "RentHub Verification OTP";
        String body = String.format("Hi,\n\nYour one-time passcode (OTP) for verification is: %s\nThis OTP is valid for 10 minutes. Do not share this with anyone.\n\nRentHub Security",
                otp);
        sendEmail(toEmail, subject, body);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "Password Reset Request - RentHub";
        String body = String.format("Hi,\n\nYou requested a password reset. Please click the link below to set a new password:\n%s\n\nIf you did not request this, please ignore this email.\n\nRentHub Team",
                resetLink);
        sendEmail(toEmail, subject, body);
    }
}
