package com.renthub.scheduler;

import com.renthub.booking.model.entity.Booking;
import com.renthub.booking.model.entity.BookingStatus;
import com.renthub.booking.repository.BookingRepository;
import com.renthub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    /**
     * Auto-completes active/approved bookings whose rental period has ended.
     * Runs every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void autoCompleteRentals() {
        log.info("Cron job: Checking for completed rentals...");
        LocalDateTime now = LocalDateTime.now();
        List<Booking> activeRentals = bookingRepository.findAll().stream()
                .filter(b -> (b.getStatus() == BookingStatus.APPROVED || b.getStatus() == BookingStatus.ACTIVE) && b.getEndDate().isBefore(now))
                .toList();

        for (Booking booking : activeRentals) {
            log.info("Auto-completing booking ID: {}", booking.getId());
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            // Notify customer & owner
            notificationService.createInAppNotification(booking.getCustomer().getId(),
                    "Rental Completed",
                    String.format("Your rental period for '%s' has officially completed.", booking.getEquipment().getTitle()));
            notificationService.createInAppNotification(booking.getEquipment().getOwner().getId(),
                    "Rental Completed",
                    String.format("The rental of your item '%s' has officially completed.", booking.getEquipment().getTitle()));
        }
    }

    /**
     * Sends rental start reminders to customers renting equipment starting tomorrow.
     * Runs every day at 8:00 AM.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void sendRentalReminders() {
        log.info("Cron job: Processing daily rental reminders...");
        LocalDateTime tomorrowStart = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrowEnd = tomorrowStart.plusDays(1);

        List<Booking> startingTomorrow = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.APPROVED && 
                        b.getStartDate().isAfter(tomorrowStart) && b.getStartDate().isBefore(tomorrowEnd))
                .toList();

        for (Booking booking : startingTomorrow) {
            log.info("Sending reminder email for booking ID: {}", booking.getId());
            notificationService.sendBookingReminderEmail(
                    booking.getCustomer().getEmail(),
                    booking.getCustomer().getFirstName(),
                    booking.getEquipment().getTitle(),
                    booking.getStartDate().toLocalDate().toString()
            );
        }
    }
}
