package com.renthub.review.service;

import com.renthub.review.model.entity.Review;

import java.util.List;

public interface ReviewService {
    Review createReview(Long bookingId, int rating, String comment);
    List<Review> getReviewsForEquipment(Long equipmentId);
    void moderateReview(Long id, boolean approve, String moderationComment);
    List<Review> getPendingModerationReviews();
}
