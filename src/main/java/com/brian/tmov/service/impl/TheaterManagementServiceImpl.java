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
    @Override
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void autoSyncMovies() {
        log.info("開始執行每日電影同步任務...");

        JsonNode nowPlaying = tmdbDiscoverService.getNowPlayingMovies(1);
        if (nowPlaying == null || !nowPlaying.has("results")) return;

        List<Long> currentTmdbIds = new ArrayList<>();

        for (JsonNode node : nowPlaying.get("results")) {
            Long tmdbId = node.get("id").asLong();
            currentTmdbIds.add(tmdbId);

            if (!theaterMovieRepository.existsByTmdbId(tmdbId)) {
                try {
                    addMovie(tmdbId);
                    log.info("自動上架電影: {}", node.path("title").asText());
                } catch (Exception e) {
                    log.error("自動上架失敗 ID: {}", tmdbId, e);
                }
            }
        }

        List<TheaterMovieEntity> allMovies = theaterMovieRepository.findAllByStatus("SHOWING");
        for (TheaterMovieEntity movie : allMovies) {
            // 下架舊片
            if (!currentTmdbIds.contains(movie.getTmdbId())) {
                removeMovie(movie.getTmdbId());
                log.info("自動下架: {}", movie.getTitle());
            } else {
                // 若是續映電影，檢查並補齊未來的場次 (例如第 7 天)
                generateMockSchedules(movie);
            }
        }
        cleanupExpiredSchedules();

        log.info("電影同步任務完成");
    }

    // 直接在資料庫層級刪除過期場次
    private void cleanupExpiredSchedules() {
        LocalDate today = LocalDate.now();

        // 使用 Repository 新增的刪除方法
        theaterScheduleRepository.deleteByShowDateBefore(today);

        log.info("已清理 {} 之前的過期場次", today);
    }

    // --- 輔助：產生假場次 ---
    private void generateMockSchedules(TheaterMovieEntity movie) {
        List<TheaterHallEntity> allHalls = theaterHallRepository.findAll();
        if (allHalls.isEmpty()) return;

        LocalDate today = LocalDate.now();

        // 產生未來 7 天 (含今天)
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);

            // 先檢查這部電影在這一天是否已經有排程了
            List<TheaterScheduleEntity> existingSchedules = theaterScheduleRepository.findByMovieAndDate(movie.getTmdbId(), date);
            if (!existingSchedules.isEmpty()) {
                // 如果這一天已經有場次，就跳過 (避免重複產生)
                continue;
            }

            // 如果這一天沒場次 (例如新的一天)，就開始排程
            for (TheaterHallEntity hall : allHalls) {

                int showsCount = getShowCountByHallType(hall.getType());

                // 計算每場間隔
                int startHour = 10;
                int totalMinutes = (24 - startHour) * 60;
                int intervalMinutes = totalMinutes / Math.max(1, showsCount);

                for (int j = 0; j < showsCount; j++) {
                    int minutesFromStart = j * intervalMinutes;
                    int hour = startHour + (minutesFromStart / 60);
                    int minute = (minutesFromStart % 60);
                    minute = (minute / 15) * 15;

                    if (hour >= 24) break;

                    LocalTime time = LocalTime.of(hour, minute);
                    int price = getPriceByHallType(hall.getType());

                    TheaterScheduleEntity schedule = new TheaterScheduleEntity(movie, hall, date, time, price);
                    theaterScheduleRepository.save(schedule);
                }
            }
        }
    }

    // 定義各廳場次數量
    private int getShowCountByHallType(String type) {
        return switch (type) {
            case "Digital-A" -> 10; // 數位廳：10 場
            case "Digital-B" -> 10;
            case "3D" -> 7;       // 3D 廳：7 場
            case "IMAX" -> 5;     // IMAX 廳：5 場
            case "GoldClass" -> 2;// Gold Class：2 場
            default -> 5;
        };
    }

    // 定義各廳票價
    private int getPriceByHallType(String type) {
        return switch (type) {
            case "GoldClass" -> 620;
            case "IMAX" -> 460;
            case "3D" -> 380;
            default -> 330;
        };
    }

    // --- 查詢已訂位座位 ---
    private List<String> findBookedSeats(TheaterScheduleEntity schedule) {
        List<BookingEntity> bookings = bookingRepository.findBookedSeats(schedule.getId());

        return bookings.stream()
                .map(BookingEntity::getSeats)
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> java.util.Arrays.stream(s.split(",")))
                .collect(Collectors.toList());
    }
}
