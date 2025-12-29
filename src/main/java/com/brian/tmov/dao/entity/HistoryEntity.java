package com.brian.tmov.dao.entity;

import com.brian.tmov.enums.MediaType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "history", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "tmdb_id", "media_type"})
})
@Data
@NoArgsConstructor
public class HistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "watched_at")
    private LocalDateTime watchedAt;

    public HistoryEntity(MemberEntity member, Long tmdbId, MediaType mediaType) {
        this.member = member;
        this.tmdbId = tmdbId;
        this.mediaType = mediaType;
        this.watchedAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.watchedAt = LocalDateTime.now();
    }
}
