package com.brian.tmov.dao.repository;

import com.brian.tmov.dao.entity.BookingEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

//    查詢某個會員的所有訂單，按時間倒序
    @Query("SELECT b FROM BookingEntity b WHERE b.member.email = :email ORDER BY b.createdAt DESC")
    List<BookingEntity> findAllByEmail(String email);

//    查詢位子
    @Query("SELECT b FROM BookingEntity b WHERE b.scheduleId = :scheduleId AND b.status <> 'CANCELLED'")
    List<BookingEntity> findBookedSeats(Long scheduleId);

//    查詢是否同時交易
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BookingEntity b WHERE b.scheduleId = :scheduleId AND b.status <> 'CANCELLED'")
    List<BookingEntity> findBookedSeatsForUpdate(Long scheduleId);

    // 批次查詢
    @Query("SELECT b FROM BookingEntity b WHERE b.scheduleId IN :scheduleIds AND b.status <> 'CANCELLED'")
    List<BookingEntity> findBookedSeatsBatch(List<Long> scheduleIds);
}
