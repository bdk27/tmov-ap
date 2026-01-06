package com.brian.tmov.dao.repository;

import com.brian.tmov.dao.entity.TheaterMovieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheaterMovieRepository extends JpaRepository<TheaterMovieEntity, Long> {

    boolean existsByTmdbId(Long tmdbId);
    List<TheaterMovieEntity> findAllByStatus(String status);
}
