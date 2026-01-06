package com.brian.tmov.dao.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "theater_schedule")
@Data
@NoArgsConstructor
public class TheaterScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "movie_id", nullable = false)
    private TheaterMovieEntity movie;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hall_id", nullable = false)
    private TheaterHallEntity hall;

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "show_time", nullable = false)
    private LocalTime showTime;

    private Integer price;

    public TheaterScheduleEntity(TheaterMovieEntity movie, TheaterHallEntity hall, LocalDate showDate, LocalTime showTime, Integer price) {
        this.movie = movie;
        this.hall = hall;
        this.showDate = showDate;
        this.showTime = showTime;
        this.price = price;
    }
}
