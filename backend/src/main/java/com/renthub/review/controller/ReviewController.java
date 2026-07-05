package com.renthub.review.controller;

import com.renthub.common.dto.ApiResponse;
import com.renthub.review.model.entity.Review;
import com.renthub.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews & Ratings Feedback", description = "Endpoints for leaving equipment reviews, viewing ratings, and administrative moderation panels.")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/v1/reviews")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Submit rating and review", description = "Leaves a rating (1-5 stars) and comments on a booking. Renter authorization required.")
    public ResponseEntity<ApiResponse<Review>> createReview(
            @RequestParam Long bookingId,
            @RequestParam int rating,
            @RequestParam(required = false) String comment) {
        Review review = reviewService.createReview(bookingId, rating, comment);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(review, "Review submitted successfully."));
    }

    @GetMapping("/api/v1/equipment/{equipmentId}/reviews")
    @Operation(summary = "Get reviews for equipment", description = "Retrieves all public reviews left for a specific equipment listing.")
    public ResponseEntity<ApiResponse<List<Review>>> getReviewsForEquipment(@PathVariable Long equipmentId) {
        List<Review> reviews = reviewService.getReviewsForEquipment(equipmentId);
        return ResponseEntity.ok(ApiResponse.success(reviews, "Reviews fetched successfully."));
    }

    @PatchMapping("/api/v1/admin/reviews/{id}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Moderate review", description = "Approves or disapproves a review comment. Requires ADMIN role.")
    public ResponseEntity<ApiResponse<Void>> moderateReview(
            @PathVariable Long id,
            @RequestParam boolean approve,
            @RequestParam(required = false) String comment) {
        reviewService.moderateReview(id, approve, comment);
        return ResponseEntity.ok(ApiResponse.success(null, "Review moderated successfully."));
    }

    @GetMapping("/api/v1/admin/reviews/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List pending moderation reviews", description = "Retrieves all reviews waiting for admin moderation. Requires ADMIN role.")
    public ResponseEntity<ApiResponse<List<Review>>> getPendingReviews() {
        List<Review> reviews = reviewService.getPendingModerationReviews();
        return ResponseEntity.ok(ApiResponse.success(reviews, "Pending reviews fetched successfully."));
    }
}
