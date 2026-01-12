package com.brian.tmov.dto.response;

import com.brian.tmov.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Taipei")
        LocalDateTime createdAt,

        Long bookingId,

        Long tmdbId,

        String movieTitle,

        String posterUrl,

        String cinemaName,

        LocalDate showDate,

        String showTime,

        String seats,

        Integer ticketCount,

        Integer totalPrice,

        BookingStatus status
) {
}
