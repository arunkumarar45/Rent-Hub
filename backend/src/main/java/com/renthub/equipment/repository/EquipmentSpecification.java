package com.renthub.equipment.repository;

import com.renthub.equipment.model.entity.Equipment;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class EquipmentSpecification {

    public static Specification<Equipment> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return null;
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Equipment> hasLocation(String location) {
        return (root, query, cb) -> {
            if (location == null || location.trim().isEmpty()) return null;
            return cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase().trim() + "%");
        };
    }

    public static Specification<Equipment> hasCategorySlug(String slug) {
        return (root, query, cb) -> {
            if (slug == null || slug.trim().isEmpty()) return null;
            return cb.equal(root.get("category").get("slug"), slug);
        };
    }

    public static Specification<Equipment> isPriceBetween(Integer minPrice, Integer maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return null;
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("dailyRate"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("dailyRate"), minPrice);
            }
            return cb.lessThanOrEqualTo(root.get("dailyRate"), maxPrice);
        };
    }

    public static Specification<Equipment> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return null;
            return cb.equal(root.get("isActive"), active);
        };
    }

    public static Specification<Equipment> isAvailable(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null || end == null) return null;
            
            // Subquery to find equipment IDs that are booked in this range
            // We use reflection or full qualified names if Booking is not yet fully compiled
            // Here, we can query bookings tables using a join, or a subquery. 
            // The table 'bookings' has columns: equipment_id, start_date, end_date, status.
            // Using a Subquery:
            // select b.equipment.id from Booking b where b.startDate < :end and b.endDate > :start and b.status NOT IN ('CANCELLED', 'REJECTED')
            
            // To write this cleanly without circular imports causing check issues, we can query it via root.join
            // or just build a standard subquery against the Booking entity.
            try {
                Class<?> bookingClass = Class.forName("com.renthub.booking.model.entity.Booking");
                Subquery<?> subquery = query.subquery(bookingClass);
                var bookingRoot = subquery.from(bookingClass);
                subquery.select(bookingRoot.get("equipment").get("id"));
                
                var statusEnumClass = Class.forName("com.renthub.booking.model.entity.BookingStatus");
                var cancelledEnum = Enum.valueOf((Class<Enum>) statusEnumClass, "CANCELLED");
                var rejectedEnum = Enum.valueOf((Class<Enum>) statusEnumClass, "REJECTED");

                subquery.where(
                    cb.and(
                        cb.equal(bookingRoot.get("equipment").get("id"), root.get("id")),
                        cb.notEqual(bookingRoot.get("status"), cancelledEnum),
                        cb.notEqual(bookingRoot.get("status"), rejectedEnum),
                        cb.lessThan(bookingRoot.get("startDate"), end),
                        cb.greaterThan(bookingRoot.get("endDate"), start)
                    )
                );
                return cb.not(cb.exists(subquery));
            } catch (ClassNotFoundException e) {
                // If Booking is not compiled yet, return null (fallback during modular building)
                return null;
            }
        };
    }
}
