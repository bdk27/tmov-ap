package com.brian.tmov.dao.repository;

import com.brian.tmov.dao.entity.FavoriteEntity;
import com.brian.tmov.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {

    // 檢查某個會員是否收藏了某部片
    boolean existsByMemberEmailAndTmdbIdAndMediaType(String email, Long tmdbId, MediaType mediaType);

    // 刪除收藏
    void deleteByMemberEmailAndTmdbIdAndMediaType(String email, Long tmdbId, MediaType mediaType);

    // 查詢特定收藏
    Optional<FavoriteEntity> findByMemberEmailAndTmdbIdAndMediaType(String email, Long tmdbId, MediaType mediaType);
}
