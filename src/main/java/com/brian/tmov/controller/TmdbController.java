package com.brian.tmov.controller;

import com.brian.tmov.dto.request.TmdbSearchQueryRequest;
import com.brian.tmov.service.TmdbDetailService;
import com.brian.tmov.service.TmdbDiscoverService;
import com.brian.tmov.service.TmdbSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "TMDB 資料整合", description = "負責與 TMDB API 介接，提供電影、影集、人物等資訊")
@RestController
@RequestMapping("/api/tmdb")
public class TmdbController {

    @Autowired
    private TmdbSearchService tmdbSearchService;

    @Autowired
    private TmdbDiscoverService tmdbDiscoverService;

    @Autowired
    private TmdbDetailService tmdbDetailService;

    @Operation(summary = "搜尋", description = "搜尋電影、影集或人物")
    @GetMapping("/search")
    public ResponseEntity<JsonNode> search(@Valid TmdbSearchQueryRequest query) {
        return ResponseEntity.ok(tmdbSearchService.search(query));
    }

    @Operation(summary = "隨機背景", description = "取得隨機一張熱門項目的背景圖 (用於首頁 Hero)")
    @GetMapping("/popular-backdrop")
    public ResponseEntity<Map<String, String>> getRandomPopularBackdrop(
            @RequestParam(defaultValue = "movie") String category
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getRandomPopularBackdrops(category));
    }

    @Operation(summary = "趨勢", description = "取得今日或本週的熱門趨勢")
    @GetMapping("/trending")
    public ResponseEntity<JsonNode> getTrendingMovies(
            @RequestParam(value = "time_window", defaultValue = "day") String timeWindow,
            @RequestParam(value = "page", defaultValue = "1") Integer page
   ) {
        return ResponseEntity.ok(tmdbDiscoverService.getTrendingAll(timeWindow, page));
    }

    @Operation(summary = "熱門電影", description = "取得目前的熱門電影列表")
    @GetMapping("/popular-movies")
    public ResponseEntity<JsonNode> getPopularMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularMovies(page));
    }

    @Operation(summary = "熱門電視節目", description = "取得目前的熱門電視節目列表")
    @GetMapping("/popular-tv")
    public ResponseEntity<JsonNode> getPopularTv(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularTv(page));
    }

    @Operation(summary = "熱門動畫", description = "取得熱門動畫列表")
    @GetMapping("/popular-anime")
    public ResponseEntity<JsonNode> getPopularAnimation(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularAnimation(page));
    }

    @Operation(summary = "熱門電視劇", description = "取得熱門電視劇列表")
    @GetMapping("/popular-drama")
    public ResponseEntity<JsonNode> getPopularDrama(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularDrama(page));
    }

    @Operation(summary = "熱門綜藝", description = "取得熱門綜藝節目列表")
    @GetMapping("/popular-variety")
    public ResponseEntity<JsonNode> getPopularVariety(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularVariety(page));
    }

    @Operation(summary = "熱門紀錄片", description = "取得熱門紀錄片列表")
    @GetMapping("/popular-documentary")
    public ResponseEntity<JsonNode> getPopularComedy(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularDocumentary(page));
    }

    @Operation(summary = "熱門兒童節目", description = "取得熱門兒童節目列表")
    @GetMapping("/popular-children")
    public ResponseEntity<JsonNode> getPopularChildren(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularChildren(page));
    }

    @Operation(summary = "熱門脫口秀", description = "取得熱門脫口秀列表")
    @GetMapping("/popular-talkShow")
    public ResponseEntity<JsonNode> getPopularTalkShow(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularTalkShow(page));
    }

    @Operation(summary = "熱門人物", description = "取得目前的熱門人物列表")
    @GetMapping("/popular-person")
    public ResponseEntity<JsonNode> getPopularPerson(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularPerson(page));
    }

    @Operation(summary = "即將上映", description = "取得即將上映的電影列表")
    @GetMapping("/upcoming")
    public ResponseEntity<JsonNode> getUpcomingMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getUpcomingMovies(page));
    }

    @Operation(summary = "預告片", description = "取得指定電影的預告片 URL")
    @GetMapping("/movie/{id}/trailer")
    public ResponseEntity<JsonNode> getMovieTrailer(@PathVariable Long id) {
        String url = tmdbDiscoverService.getMovieTrailer(id);
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        if (url != null) {
            json.put("trailerUrl", url);
        } else {
            json.putNull("trailerUrl");
        }
        return ResponseEntity.ok(json);
    }

    @Operation(summary = "現正熱映", description = "取得台灣地區現正熱映的電影列表")
    @GetMapping("/now-playing")
    public ResponseEntity<JsonNode> getNowPlayingMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getNowPlayingMovies(page));
    }

    @Operation(summary = "好評推薦", description = "取得評價最高的電影列表")
    @GetMapping("/top-rated")
    public ResponseEntity<JsonNode> getTopRatedMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getTopRatedMovies(page));
    }

    @Operation(summary = "電影詳情", description = "取得指定電影的詳細資料")
    @GetMapping("/movie/{id}")
    public ResponseEntity<JsonNode> getMovieDetail(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbDetailService.getMovieDetail(id));
    }

    @Operation(summary = "電視節目詳情", description = "取得指定電視節目的詳細資料")
    @GetMapping("/tv/{id}")
    public ResponseEntity<JsonNode> getTvDetail(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbDetailService.getTvDetail(id));
    }

    @Operation(summary = "人物詳情", description = "取得指定人物的詳細資料")
    @GetMapping("/person/{id}")
    public ResponseEntity<JsonNode> getPersonDetail(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbDetailService.getPersonDetail(id));
    }
}
