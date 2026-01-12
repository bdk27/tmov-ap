package com.brian.tmov.service;

import com.brian.tmov.dao.entity.BookingEntity;
import com.brian.tmov.dto.request.BookingRequest;
import com.brian.tmov.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {

    // 建立訂單
    BookingEntity createBooking(String email, BookingRequest request);

    // 取得我的訂單
    List<BookingResponse> getMyBookings(String email);

    // 模擬付款 (將狀態改為 PAID)
    void payBooking(String email, Long bookingId);

    // 取消訂單
    void cancelBooking(String email, Long bookingId);
}
