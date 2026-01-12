package com.brian.tmov.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record BookingRequest(
        @NotNull(message = "TMDB ID 不能為空")
        Long tmdbId,

        @NotBlank(message = "電影名稱不能為空")
        String movieTitle,

        @NotNull(message = "場次 ID 不能為空")
        Long scheduleId,

        String posterUrl,

        @NotBlank(message = "戲院名稱不能為空")
        String cinemaName,

        @NotNull(message = "日期不能為空")
        LocalDate showDate,

        @NotBlank(message = "時間不能為空")
        String showTime,

        @NotNull(message = "請選擇座位")
        List<String> seats, // 前端傳陣列 ["A1", "A2"]，後端轉字串

        Integer totalPrice
) {}
