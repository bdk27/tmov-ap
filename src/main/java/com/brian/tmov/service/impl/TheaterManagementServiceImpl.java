package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.dao.entity.BookingEntity;
import com.brian.tmov.dao.entity.TheaterHallEntity;
import com.brian.tmov.dao.entity.TheaterMovieEntity;
import com.brian.tmov.dao.entity.TheaterScheduleEntity;
import com.brian.tmov.dao.repository.BookingRepository;
import com.brian.tmov.dao.repository.TheaterHallRepository;
import com.brian.tmov.dao.repository.TheaterScheduleRepository;
import com.brian.tmov.dao.repository.TheaterMovieRepository;
import com.brian.tmov.dto.response.ScheduleResponse;
import com.brian.tmov.service.TheaterManagementService;
import com.brian.tmov.service.TmdbDiscoverService;
import com.brian.tmov.service.TmdbGetImageService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TheaterManagementServiceImpl implements TheaterManagementService {

    @Autowired
    private TheaterMovieRepository theaterMovieRepository;

    @Autowired
    private TheaterHallRepository theaterHallRepository;

    @Autowired
    private TheaterScheduleRepository theaterScheduleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private TmdbGetImageService tmdbGetImageService;

    @Autowired
    private TmdbDiscoverService tmdbDiscoverService;

    // 上架電影
    @Transactional
    public void addMovie(Long tmdbId) {
        if (theaterMovieRepository.existsByTmdbId(tmdbId)) {
            throw new IllegalArgumentException("此電影已在上映中");
        }

        // 呼叫 TMDB API
        JsonNode json = tmdbClient.get(new String[]{"movie", String.valueOf(tmdbId)}, java.util.Map.of("language", "zh-TW"));

        String title = json.path("title").asText();
        String posterPath = json.path("poster_path").asText();
        String backdropPath = json.path("backdrop_path").asText();
        String dateStr = json.path("release_date").asText();
        LocalDate releaseDate = (dateStr != null && !dateStr.isBlank()) ? LocalDate.parse(dateStr) : LocalDate.now();

        TheaterMovieEntity movie = new TheaterMovieEntity(
                tmdbId,
                title,
                tmdbGetImageService.getDefaultPosterUrl(posterPath),
                tmdbGetImageService.getDefaultBackdropUrl(backdropPath),
                releaseDate
        );

        theaterMovieRepository.save(movie);

        // 自動產生未來 7天的隨機場次 (模擬用)
        generateMockSchedules(movie);
    }

    // 下架電影 (刪除電影與關聯場次)
    @Transactional
    public void removeMovie(Long tmdbId) {
        // 由於我們設定了 ON DELETE CASCADE，刪除 Movie 會自動刪除 Schedule
        TheaterMovieEntity movie = theaterMovieRepository.findAll().stream()
                .filter(m -> m.getTmdbId().equals(tmdbId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("找不到此上映電影"));

        theaterMovieRepository.delete(movie);
    }

    // 取得目前上映電影列表
    public List<TheaterMovieEntity> getNowShowing() {
        return theaterMovieRepository.findAllByStatus("SHOWING");
    }

    // 取得某電影的場次與座位資訊
    public List<ScheduleResponse> getSchedules(Long tmdbId, LocalDate date) {
        List<TheaterScheduleEntity> schedules = theaterScheduleRepository.findByMovieAndDate(tmdbId, date);

        return schedules.stream().map(s -> new ScheduleResponse(
                s.getId(),
                s.getMovie().getTmdbId(),
                s.getMovie().getTitle(),
                s.getHall().getName(),
                s.getHall().getType(),
                s.getShowDate(),
                s.getShowTime(),
                s.getPrice(),
                s.getHall().getRowCount(),
                s.getHall().getColCount(),
                findBookedSeats(s)
        )).collect(Collectors.toList());
    }

    /**
     * 每天凌晨 04:00 自動同步上映電影
     * 1. 抓取 TMDB 現正熱映
     * 2. 自動上架新片
     * 3. 自動下架舊片
     */
    @Scheduled(cron = "0 0 4 * * ?") // 每天 04:00:00 執行
    @Transactional
    public void autoSyncMovies() {
        log.info("開始執行每日電影同步任務...");

        // 1. 取得 TMDB 現正熱映 (抓第 1 頁即可，通常是最新最熱的)
        JsonNode nowPlaying = tmdbDiscoverService.getNowPlayingMovies(1);
        if (nowPlaying == null || !nowPlaying.has("results")) return;

        List<Long> currentTmdbIds = new ArrayList<>();

        // 2. 遍歷結果，自動上架
        for (JsonNode node : nowPlaying.get("results")) {
            Long tmdbId = node.get("id").asLong();
            currentTmdbIds.add(tmdbId);

            // 如果資料庫沒有這部片，就自動上架
            if (!theaterMovieRepository.existsByTmdbId(tmdbId)) {
                try {
                    addMovie(tmdbId); // 重用原本的上架邏輯 (含自動排場次)
                    log.info("自動上架電影: {}", node.path("title").asText());
                } catch (Exception e) {
                    log.error("自動上架失敗 ID: {}", tmdbId, e);
                }
            }
        }

        // 3. 自動下架過期電影
        List<TheaterMovieEntity> allMovies = theaterMovieRepository.findAllByStatus("SHOWING");

        for (TheaterMovieEntity movie : allMovies) {
            // 如果本地的電影 ID 不在 TMDB 這次回傳的列表中，就視為已下檔
            if (!currentTmdbIds.contains(movie.getTmdbId())) {
                removeMovie(movie.getTmdbId()); // 重用下架邏輯
                log.info("自動下架 (已不在熱映榜): {}", movie.getTitle());
            }
        }

        log.info("電影同步任務完成");
    }

    // --- 輔助：產生假場次 ---
    private void generateMockSchedules(TheaterMovieEntity movie) {
        List<TheaterHallEntity> halls = theaterHallRepository.findAll();
        System.out.println("halls: " + halls);
        if (halls.isEmpty()) return;

        LocalDate today = LocalDate.now();
        // 產生 7 天份
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            // 每天 3~5 場
            int shows = 3 + (int)(Math.random() * 3);

            for (int j = 0; j < shows; j++) {
                // 隨機選廳
                TheaterHallEntity hall = halls.get((int)(Math.random() * halls.size()));
                // 時間：10:00 ~ 22:00
                int hour = 10 + (j * 3);
                LocalTime time = LocalTime.of(Math.min(hour, 23), 0);

                TheaterScheduleEntity schedule = new TheaterScheduleEntity(movie, hall, date, time, 300);
                theaterScheduleRepository.save(schedule);
            }
        }
    }

    // --- 查詢已訂位座位 ---
    private List<String> findBookedSeats(TheaterScheduleEntity schedule) {
        // 將 LocalTime (10:30:00) 轉為 String (10:30) 以符合 BookingEntity 的儲存格式
        String timeStr = schedule.getShowTime().toString();
        if (timeStr.length() > 5) {
            timeStr = timeStr.substring(0, 5);
        }

        // 查詢符合 電影ID + 日期 + 時間 的所有訂單
        List<BookingEntity> bookings = bookingRepository.findBookedSeats(
                schedule.getMovie().getTmdbId(),
                schedule.getShowDate(),
                timeStr
        );

        // 將訂單中的座位字串 (e.g. "A1,A2") 拆解並合併成一個 List
        return bookings.stream()
                .map(BookingEntity::getSeats)
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> java.util.Arrays.stream(s.split(",")))
                .collect(Collectors.toList());
    }
}
