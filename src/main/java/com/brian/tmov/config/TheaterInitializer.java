package com.brian.tmov.config;

import com.brian.tmov.dao.entity.TheaterHallEntity;
import com.brian.tmov.dao.repository.TheaterHallRepository;
import com.brian.tmov.service.TheaterManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TheaterInitializer implements CommandLineRunner {

    @Autowired
    private TheaterHallRepository theaterHallRepository;

    @Autowired
    private TheaterManagementService cinemaService;

    @Override
    public void run(String... args) throws Exception {
        // 初始化影廳資料 (如果資料庫是空的)
        if (theaterHallRepository.count() == 0) {
            System.out.println("偵測到影廳資料為空，開始初始化全台影城...");
            initTheaters();
        }

        // 初始化電影與場次 (如果還沒有電影)
        // 這裡會觸發我們寫好的 autoSyncMovies，去 TMDB 抓熱映電影並排程
        try {
            System.out.println("正在同步 TMDB 現正熱映電影...");
            cinemaService.autoSyncMovies();
        } catch (Exception e) {
            System.out.println("初始化電影失敗: " + e.getMessage());
        }
    }

    private void initTheaters() {
        List<TheaterHallEntity> halls = new ArrayList<>();

        // --- 台北 ---
        halls.add(createHall("台北信義威秀 - A廳", 15, 20, "數位"));
        halls.add(createHall("台北信義威秀 - IMAX廳", 18, 25, "IMAX"));
        halls.add(createHall("台北信義威秀 - 4DX廳", 10, 12, "4DX"));
        halls.add(createHall("台北京站威秀 - 1廳", 12, 18, "數位"));
        halls.add(createHall("台北京站威秀 - 6廳", 10, 15, "數位"));
        halls.add(createHall("美麗華大直影城 - IMAX廳", 20, 30, "IMAX"));
        halls.add(createHall("美麗華大直影城 - 標準廳", 12, 16, "數位"));

        // --- 新北 ---
        halls.add(createHall("板橋大遠百威秀 - IMAX廳", 16, 22, "IMAX"));
        halls.add(createHall("板橋大遠百威秀 - B廳", 12, 16, "數位"));
        halls.add(createHall("林口MITSUI OUTLET威秀 - MAPPA廳", 8, 10, "Gold Class"));

        // --- 桃園/新竹 ---
        halls.add(createHall("桃園統領威秀 - 9廳", 10, 14, "數位"));
        halls.add(createHall("新竹巨城威秀 - IMAX廳", 14, 20, "IMAX"));

        // --- 台中 ---
        halls.add(createHall("台中大遠百威秀 - 1廳", 15, 20, "數位"));
        halls.add(createHall("台中大遠百威秀 - IMAX廳", 16, 24, "IMAX"));
        halls.add(createHall("台中站前秀泰 - S1廳", 20, 25, "巨幕"));

        // --- 台南/高雄 ---
        halls.add(createHall("台南南紡威秀 - IMAX廳", 16, 22, "IMAX"));
        halls.add(createHall("高雄大遠百威秀 - 3廳", 12, 18, "數位"));
        halls.add(createHall("高雄大遠百威秀 - 4DX廳", 8, 12, "4DX"));
        halls.add(createHall("高雄夢時代秀泰 - 1廳", 10, 15, "數位"));

        theaterHallRepository.saveAll(halls);
        System.out.println("影廳初始化完成，共建立 " + halls.size() + " 個影廳。");
    }

    private TheaterHallEntity createHall(String name, int row, int col, String type) {
        TheaterHallEntity hall = new TheaterHallEntity();
        hall.setName(name);
        hall.setRowCount(row);
        hall.setColCount(col);
        hall.setType(type);
        return hall;
    }
}
