package com.brian.tmov.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleResponse(

        Long scheduleId,

        Long tmdbId,

        String movieTitle,

        String hallName,

        String hallType,

        LocalDate showDate,

        LocalTime showTime,

        Integer price,

        // 座位資訊 (讓前端畫格子)
        int rowCount,

        int colCount,

        // 已售出的座位 (例如: ["A1", "B5"]) - 這部分需要從 Booking 表查詢
        java.util.List<String> bookedSeats
) {}
