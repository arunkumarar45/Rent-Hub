package com.renthub.review.service;

import com.renthub.auth.model.entity.User;
import com.renthub.auth.repository.UserRepository;
import com.renthub.booking.model.entity.Booking;
import com.renthub.booking.model.entity.BookingStatus;
import com.renthub.booking.repository.BookingRepository;
import com.renthub.common.exception.BadRequestException;
import com.renthub.common.exception.ForbiddenException;
import com.renthub.common.exception.ResourceNotFoundException;
import com.renthub.common.util.SecurityUtils;
import com.renthub.notification.service.NotificationService;
import com.renthub.review.model.entity.Review;
import com.renthub.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public Review createReview(Long bookingId, int rating, String comment) {
        String email = SecurityUtils.getCurrentUserEmail();
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new ForbiddenException("You can only review equipment that you have rented yourself.");
        }

        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5.");
        }

        Review review = Review.builder()
                .booking(booking)
                .reviewer(customer)
                .rating(rating)
                .comment(comment)
                .isModerated(false)
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Saved review ID: {} for booking ID: {}", saved.getId(), bookingId);

        // Notify owner
        notificationService.createInAppNotification(booking.getEquipment().getOwner().getId(),
                "New Equipment Review",
                String.format("Customer %s left a %d-star review on '%s'.", customer.getFirstName(), rating, booking.getEquipment().getTitle()));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsForEquipment(Long equipmentId) {
        return reviewRepository.findByBookingEquipmentId(equipmentId);
    }

    @Override
    @Transactional
    public void moderateReview(Long id, boolean approve, String moderationComment) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

        review.setIsModerated(true);
        review.setModerationComment(moderationComment);

        if (!approve) {
            log.info("Flagging review ID: {} as content violation", id);
            review.setComment("[This review has been removed due to content violation guidelines]");
        } else {
            log.info("Approved review ID: {}", id);
        }

        reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getPendingModerationReviews() {
        return reviewRepository.findByIsModeratedFalse();
    }
}
