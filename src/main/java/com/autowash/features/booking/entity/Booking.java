package com.autowash.features.booking.entity;

import com.autowash.features.car.entity.Car;
import com.autowash.features.user.entity.User;

import com.autowash.features.booking.enums.BookingStatus;
import com.autowash.features.booking.enums.CancelRequestStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "BOOKINGS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "Booking_code", nullable = false, unique = true)
    private String bookingCode;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Car vehicle;

    @Column(name = "Scheduled_start_time", nullable = false)
    private LocalDateTime scheduledStartTime;

    @Column(name = "Expected_end_time")
    private LocalDateTime expectedEndTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "Bay_number")
    private Integer bayNumber;

    @Builder.Default
    @Column(name = "Is_late", nullable = false)
    private Boolean late = false;

    @Column(name = "Customer_note", columnDefinition = "TEXT")
    private String customerNote;

    @Column(name = "Total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "Qr_content", nullable = false, unique = true)
    private String qrContent;

    @Builder.Default
    @Column(name = "Qr_used", nullable = false)
    private Boolean qrUsed = false;

    @Column(name = "Arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "Wash_started_at")
    private LocalDateTime washStartedAt;

    @Column(name = "Completed_at")
    private LocalDateTime completedAt;

    @Builder.Default
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<BookingDetail> bookingDetails = new ArrayList<>();

    @OneToOne(mappedBy = "booking")
    private Payment payment;

    @OneToOne(mappedBy = "booking")
    private Review review;

    @ManyToOne
    @JoinColumn(name = "Staff_id")
    private User staff;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_request_status")
    private CancelRequestStatus cancelRequestStatus;

    @Column(name = "cancel_request_reason", columnDefinition = "TEXT")
    private String cancelRequestReason;

    @ManyToOne
    @JoinColumn(name = "cancel_requested_by")
    private User cancelRequestedBy;

    @Column(name = "cancel_requested_at")
    private LocalDateTime cancelRequestedAt;

    @Column(name = "cancel_request_admin_note", columnDefinition = "TEXT")
    private String cancelRequestAdminNote;

    @Column(name = "Created_at")
    private LocalDateTime createdAt;

    @Column(name = "Updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


