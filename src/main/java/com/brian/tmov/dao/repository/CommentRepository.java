package com.brian.tmov.dao.repository;

import com.brian.tmov.dao.entity.CommentEntity;
import com.brian.tmov.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    @Query("SELECT c FROM CommentEntity c JOIN FETCH c.member WHERE c.tmdbId = :tmdbId AND c.mediaType = :mediaType ORDER BY c.createdAt DESC")
    List<CommentEntity> find(Long tmdbId, MediaType mediaType);
}
