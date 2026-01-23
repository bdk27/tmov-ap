package com.brian.tmov.service.impl;

import com.brian.tmov.dao.entity.BookingEntity;
import com.brian.tmov.dao.entity.MemberEntity;
import com.brian.tmov.dao.repository.BookingRepository;
import com.brian.tmov.dao.repository.MemberRepository;
import com.brian.tmov.dto.request.BookingRequest;
import com.brian.tmov.dto.response.BookingResponse;
import com.brian.tmov.enums.BookingStatus;
import com.brian.tmov.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Override
    @Transactional
    public BookingEntity createBooking(String email, BookingRequest request) {
//        驗證會員
        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("會員不存在"));

//        如果同時有別人在訂同一個場次，程式會在這裡等待(Block)，直到對方交易完成
        List<BookingEntity> existingBookings = bookingRepository.findBookedSeatsForUpdate(request.scheduleId());

//        檢查座位衝突
        Set<String> occupiedSeats = existingBookings.stream()
                .map(BookingEntity::getSeats)
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> Arrays.stream(s.split(",")))
                .collect(Collectors.toSet());

        for (String seat : request.seats()) {
            if (occupiedSeats.contains(seat)) {
                throw new IllegalArgumentException("座位 " + seat + " 剛剛被搶走了！請重新選擇。");
            }
        }

//        建立訂單
        BookingEntity booking = new BookingEntity();
        booking.setMember(member);
        booking.setScheduleId(request.scheduleId());
        booking.setTmdbId(request.tmdbId());
        booking.setMovieTitle(request.movieTitle());
        booking.setPosterUrl(request.posterUrl());
        booking.setCinemaName(request.cinemaName());
        booking.setShowDate(request.showDate());
        booking.setShowTime(request.showTime());

        // 處理座位 (List -> String)
        String seatString = String.join(",", request.seats());
        booking.setSeats(seatString);

        booking.setTicketCount(request.seats().size());
        booking.setTotalPrice(request.totalPrice());

        booking.setStatus(BookingStatus.PENDING); // 初始狀態

        return bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String email) {
        List<BookingEntity> bookings = bookingRepository.findAllByEmail(email);

        return bookings.stream().map(b -> new BookingResponse(
                b.getCreatedAt(),
                b.getId(),
                b.getTmdbId(),
                b.getMovieTitle(),
                b.getPosterUrl(),
                b.getCinemaName(),
                b.getShowDate(),
                b.getShowTime(),
                b.getSeats(),
                b.getTicketCount(),
                b.getTotalPrice(),
                b.getStatus()
        )).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void payBooking(String email, Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("訂單不存在"));

        if (!booking.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("無權操作此訂單");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("訂單狀態無法付款");
        }

        // 這裡可以串接綠界/LinePay，但我們先模擬直接成功
        booking.setStatus(BookingStatus.PAID);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void cancelBooking(String email, Long bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("訂單不存在"));

        if (!booking.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("無權操作此訂單");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }
}
