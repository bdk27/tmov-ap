package com.brian.tmov.dao.repository;

import com.brian.tmov.dao.entity.FavoriteEntity;
import com.brian.tmov.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

//    查詢使用者的所有收藏，依建立時間倒序
    @Query("SELECT f FROM FavoriteEntity f WHERE f.member.email = :email ORDER BY f.createdAt DESC")
    List<FavoriteEntity> findAllByEmail(String email);

    // 檢查某個會員是否收藏了某部片
    @Query("SELECT f FROM FavoriteEntity f " +
            "WHERE f.member.email = :email AND f.tmdbId = :tmdbId AND f.mediaType = :mediaType")
    Optional<FavoriteEntity> find(String email, Long tmdbId, MediaType mediaType);

    // 刪除收藏
    @Modifying
    @Query("DELETE FROM FavoriteEntity f " +
            "WHERE f.member.email = :email AND f.tmdbId = :tmdbId AND f.mediaType = :mediaType")
    void remove(String email, Long tmdbId, MediaType mediaType);

    // 查詢特定收藏
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FavoriteEntity f " +
            "WHERE f.member.email = :email AND f.tmdbId = :tmdbId AND f.mediaType = :mediaType")
    boolean check(String email, Long tmdbId, MediaType mediaType);
}
