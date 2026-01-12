package com.brian.tmov.dao.entity;

import com.brian.tmov.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking")
@Data
@NoArgsConstructor
public class BookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "tmdb_id")
    private Long tmdbId;

    @Column(name = "movie_title")
    private String movieTitle;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "cinema_name")
    private String cinemaName;

    @Column(name = "show_date")
    private LocalDate showDate;

    @Column(name = "show_time")
    private String showTime;

    @Column(name = "seats")
    private String seats; // 用逗號分隔，如 "A1,A2"

    @Column(name = "ticket_count")
    private Integer ticketCount;

    @Column(name = "total_price")
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BookingStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = BookingStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
