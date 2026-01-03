package com.brian.tmov.dao.repository;

import com.brian.tmov.dao.entity.HistoryEntity;
import com.brian.tmov.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoryRepository extends JpaRepository<HistoryEntity, Long> {

    // 查詢特定紀錄 (用於判斷是否需要更新時間)
    @Query("SELECT h FROM HistoryEntity h " +
            "WHERE h.member.email = :email AND h.tmdbId = :tmdbId AND h.mediaType = :mediaType")
    Optional<HistoryEntity> find(String email, Long tmdbId, MediaType mediaType);

    // 刪除紀錄
    @Modifying
    @Query("DELETE FROM HistoryEntity h " +
            "WHERE h.member.email = :email AND h.tmdbId = :tmdbId AND h.mediaType = :mediaType")
    void remove(String email, Long tmdbId, MediaType mediaType);

    // 查詢該會員的所有紀錄，按觀看時間倒序 (最新的在上面)
    @Query("SELECT h FROM HistoryEntity h WHERE h.member.email = :email ORDER BY h.watchedAt DESC")
    List<HistoryEntity> findAllByEmail(String email);

    // 清空該會員所有紀錄
    @Modifying
    @Query("DELETE FROM HistoryEntity h WHERE h.member.email = :email")
    void removeAllByEmail(String email);
}
