package com.brian.tmov.controller;

import com.brian.tmov.dto.response.ScheduleResponse;
import com.brian.tmov.service.TheaterManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "影城管理", description = "管理上映電影、查詢場次與座位狀態")
@RestController
@RequestMapping("/api/theater")
public class TheaterController {

    @Autowired
    private TheaterManagementService theaterManagementService;

    @Operation(summary = "查詢場次", description = "根據電影 ID 與日期，取得當天的場次時刻與座位狀況")
    @GetMapping("/schedules")
    public ResponseEntity<List<ScheduleResponse>> getSchedules(
            @RequestParam Long tmdbId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(theaterManagementService.getSchedules(tmdbId, date));
    }
}
