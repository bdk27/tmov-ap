package com.brian.tmov.dao.repository;

import com.brian.tmov.dao.entity.TheaterScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TheaterScheduleRepository extends JpaRepository<TheaterScheduleEntity, Long> {

    // 查詢某部電影在某個日期的所有場次
    @Query("SELECT s FROM TheaterScheduleEntity s WHERE s.movie.tmdbId = :tmdbId AND s.showDate = :date ORDER BY s.showTime ASC")
    List<TheaterScheduleEntity> findByMovieAndDate(Long tmdbId, LocalDate date);

    // 查詢某部電影所有未來的場次
    List<TheaterScheduleEntity> findByMovieIdAndShowDateGreaterThanEqualOrderByShowDateAscShowTimeAsc(Long movieId, LocalDate date);
}
