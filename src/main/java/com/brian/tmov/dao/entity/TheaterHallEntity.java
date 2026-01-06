package com.brian.tmov.dao.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "theater_hall")
@Data
public class TheaterHallEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "col_count")
    private Integer colCount;

    private String type;
}
