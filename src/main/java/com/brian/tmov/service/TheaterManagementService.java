package com.brian.tmov.service;

import com.brian.tmov.dao.entity.TheaterMovieEntity;
import com.brian.tmov.dto.response.ScheduleResponse;

import java.time.LocalDate;
import java.util.List;

public interface TheaterManagementService {

    void addMovie(Long tmdbId);

    void removeMovie(Long tmdbId);

    void autoSyncMovies();

    List<TheaterMovieEntity> getNowShowing();

    List<ScheduleResponse> getSchedules(Long tmdbId, LocalDate date);
}
