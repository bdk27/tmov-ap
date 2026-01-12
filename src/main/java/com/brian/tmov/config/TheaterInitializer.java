package com.brian.tmov.config;

import com.brian.tmov.dao.entity.TheaterHallEntity;
import com.brian.tmov.dao.repository.TheaterHallRepository;
import com.brian.tmov.service.TheaterManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TheaterInitializer implements CommandLineRunner {

    @Autowired
    private TheaterHallRepository theaterHallRepository;

    @Autowired
    private TheaterManagementService cinemaService;

    private static final int EXPECTED_HALL_COUNT = 55;

    @Override
    public void run(String... args) throws Exception {
        long currentCount = theaterHallRepository.count();

        // 1. 檢查影廳資料是否完整
        // 如果數量不對 (例如是舊的資料)，就清空重來
        if (currentCount < EXPECTED_HALL_COUNT) {
            System.out.println("偵測到影廳資料不完整 (目前 " + currentCount + " 筆)，正在重置全台影城資料...");
            theaterHallRepository.deleteAll(); // 清空舊資料
            initTheaters();
        } else {
            System.out.println("影廳資料完整 (" + currentCount + " 筆)，略過初始化。");
        }

        // 2. 初始化電影與場次
        try {
            System.out.println("正在同步 TMDB 現正熱映電影並安排場次...");
            cinemaService.autoSyncMovies();
        } catch (Exception e) {
            System.out.println("初始化電影失敗 (可能是網路問題): " + e.getMessage());
        }
    }

    private void initTheaters() {
        List<TheaterHallEntity> allHalls = new ArrayList<>();

        // 定義所有影城 (Key: 代碼, Value: 中文名稱)
        Map<String, String> cinemas = Map.ofEntries(
                Map.entry("tp-xinyi", "台北信義 TMOV 影城"),
                Map.entry("tp-titan", "台北松仁 TMOV TITAN"),
                Map.entry("tp-qsquare", "台北京站 TMOV 影城"),
                Map.entry("nt-banqiao", "板橋大遠百 TMOV 影城"),
                Map.entry("ty-tonlin", "桃園統領 TMOV 影城"),
                Map.entry("hc-bigcity", "新竹巨城 TMOV 影城"),
                Map.entry("tc-topcity", "台中大遠百 TMOV 影城"),
                Map.entry("tc-tiger", "台中 TMOV 影城"),
                Map.entry("tn-focus", "台南大遠百 TMOV 影城"),
                Map.entry("tn-dream", "台南南紡 TMOV 影城"),
                Map.entry("ks-far", "高雄大遠百 TMOV 影城")
        );

        // 為每一家影城建立 5 種標準影廳 (符合您的要求)
        for (Map.Entry<String, String> entry : cinemas.entrySet()) {
            String cinemaName = entry.getValue();

            // 1. 數位 A 廳 (小廳) - 適合排片量大的數位版
            allHalls.add(createHall(cinemaName + " - 數位 A廳", 8, 10, "Digital-A"));

            // 2. 數位 B 廳 (大廳)
            allHalls.add(createHall(cinemaName + " - 數位 B廳", 12, 18, "Digital-B"));

            // 3. 3D 廳
            allHalls.add(createHall(cinemaName + " - 3D廳", 10, 16, "3D"));

            // 4. IMAX 廳 (超大)
            allHalls.add(createHall(cinemaName + " - IMAX廳", 10, 20, "IMAX"));

            // 5. Gold Class (奢華)
            allHalls.add(createHall(cinemaName + " - Gold Class", 4, 8, "GoldClass"));
        }

        theaterHallRepository.saveAll(allHalls);
        System.out.println("影廳初始化完成，共建立 " + allHalls.size() + " 個影廳 (11家 x 5廳)。");
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
