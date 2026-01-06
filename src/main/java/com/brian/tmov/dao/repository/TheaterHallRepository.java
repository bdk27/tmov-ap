package com.brian.tmov.dao.repository;

import com.brian.tmov.dao.entity.TheaterHallEntity;
import com.brian.tmov.dao.entity.TheaterScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TheaterHallRepository extends JpaRepository<TheaterHallEntity, Long> {
}
