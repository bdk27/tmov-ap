package com.brian.tmov.controller;

import com.brian.tmov.dao.entity.BookingEntity;
import com.brian.tmov.dto.request.BookingRequest;
import com.brian.tmov.dto.response.BookingResponse;
import com.brian.tmov.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Tag(name = "訂票系統", description = "建立訂單、查詢歷史訂單與模擬付款")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Operation(summary = "建立訂單", description = "選擇場次與座位後建立新訂單 (狀態為 PENDING)")
    @PostMapping
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        BookingEntity booking = bookingService.createBooking(principal.getName(), request);
        return ResponseEntity.ok(Map.of(
                "message", "訂單建立成功",
                "bookingId", booking.getId()
        ));
    }

    @Operation(summary = "查詢我的訂單", description = "取得當前使用者的所有歷史訂單")
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getMyBookings(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(bookingService.getMyBookings(principal.getName()));
    }

    @Operation(summary = "模擬付款", description = "將訂單狀態由 PENDING 改為 PAID")
    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<?> payBooking(@PathVariable Long bookingId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        bookingService.payBooking(principal.getName(), bookingId);
        return ResponseEntity.ok(Map.of("message", "付款成功"));
    }

    @Operation(summary = "取消訂單", description = "將訂單狀態改為 CANCELLED")
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        bookingService.cancelBooking(principal.getName(), bookingId);
        return ResponseEntity.ok(Map.of("message", "訂單已取消"));
    }
}
