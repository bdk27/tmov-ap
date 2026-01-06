package com.brian.tmov.controller;

import com.brian.tmov.dao.entity.TheaterMovieEntity;
import com.brian.tmov.dto.response.ScheduleResponse;
import com.brian.tmov.service.TheaterManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/theater")
public class TheaterController {

    @Autowired
    private TheaterManagementService theaterManagementService;

    // 1. [後台] 上架電影 (傳入 TMDB ID)
    // POST /api/theater/movies?tmdbId=550
    @PostMapping("/movies")
    public ResponseEntity<?> addMovie(@RequestParam Long tmdbId) {
        theaterManagementService.addMovie(tmdbId);
        return ResponseEntity.ok(Map.of("message", "電影上架成功，已自動排定場次"));
    }

    // 2. [後台] 下架電影 (傳入 TMDB ID)
    @DeleteMapping("/movies/{tmdbId}")
    public ResponseEntity<?> removeMovie(@PathVariable Long tmdbId) {
        theaterManagementService.removeMovie(tmdbId);
        return ResponseEntity.ok(Map.of("message", "電影已下架"));
    }

    // 3. [前台] 取得目前上映中的電影
    @GetMapping("/movies")
    public ResponseEntity<List<TheaterMovieEntity>> getNowShowing() {
        return ResponseEntity.ok(theaterManagementService.getNowShowing());
    }

    // 4. [前台] 查詢某電影的場次與座位狀況
    // GET /api/theater/schedules?tmdbId=550&date=2024-01-01
    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponse>> getSchedules(
            @RequestParam Long tmdbId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(theaterManagementService.getSchedules(tmdbId, date));
    }
}
