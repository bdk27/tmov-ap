package com.brian.tmov.controller;

import com.brian.tmov.dao.entity.BookingEntity;
import com.brian.tmov.dto.request.BookingRequest;
import com.brian.tmov.dto.response.BookingResponse;
import com.brian.tmov.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // 建立訂單
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

    // 取得我的訂單列表
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getMyBookings(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(bookingService.getMyBookings(principal.getName()));
    }

    // 模擬付款
    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<?> payBooking(@PathVariable Long bookingId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        bookingService.payBooking(principal.getName(), bookingId);
        return ResponseEntity.ok(Map.of("message", "付款成功"));
    }

    // 取消訂單
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        bookingService.cancelBooking(principal.getName(), bookingId);
        return ResponseEntity.ok(Map.of("message", "訂單已取消"));
    }
}
