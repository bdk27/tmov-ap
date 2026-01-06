package com.brian.tmov.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "theater_movie")
@Data
@NoArgsConstructor
public class TheaterMovieEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(nullable = false)
    private String title;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "backdrop_url")
    private String backdropUrl;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(nullable = false)
    private String status; // "SHOWING"

    public TheaterMovieEntity(Long tmdbId, String title, String posterUrl, String backdropUrl, LocalDate releaseDate) {
        this.tmdbId = tmdbId;
        this.title = title;
        this.posterUrl = posterUrl;
        this.backdropUrl = backdropUrl;
        this.releaseDate = releaseDate;
        this.status = "SHOWING";
    }
}
