package com.brian.tmov.controller;

import com.brian.tmov.dto.request.TmdbSearchQueryRequest;
import com.brian.tmov.service.TmdbDetailService;
import com.brian.tmov.service.TmdbDiscoverService;
import com.brian.tmov.service.TmdbSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tmdb")
public class TmdbController {

    @Autowired
    private TmdbSearchService tmdbSearchService;

    @Autowired
    private TmdbDiscoverService tmdbDiscoverService;

    @Autowired
    private TmdbDetailService tmdbDetailService;

//    搜尋
    @GetMapping("/search")
    public ResponseEntity<JsonNode> search(@Valid TmdbSearchQueryRequest query) {
        return ResponseEntity.ok(tmdbSearchService.search(query));
    }

//    隨機背景
    @GetMapping("/popular-backdrop")
    public ResponseEntity<Map<String, String>> getRandomPopularBackdrop(
            @RequestParam(defaultValue = "movie") String category
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getRandomPopularBackdrops(category));
    }

//    趨勢
    @GetMapping("/trending")
    public ResponseEntity<JsonNode> getTrendingMovies(
            @RequestParam(value = "time_window", defaultValue = "day") String timeWindow,
            @RequestParam(value = "page", defaultValue = "1") Integer page
   ) {
        return ResponseEntity.ok(tmdbDiscoverService.getTrendingAll(timeWindow, page));
    }

//    熱門電影
    @GetMapping("/popular-movies")
    public ResponseEntity<JsonNode> getPopularMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularMovies(page));
    }

//    熱門電視節目
    @GetMapping("/popular-tv")
    public ResponseEntity<JsonNode> getPopularTv(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularTv(page));
    }

//    熱門動畫
    @GetMapping("/popular-anime")
    public ResponseEntity<JsonNode> getPopularAnimation(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularAnimation(page));
    }

//    熱門電視劇
    @GetMapping("/popular-drama")
    public ResponseEntity<JsonNode> getPopularDrama(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularDrama(page));
    }

//    熱門綜藝
    @GetMapping("/popular-variety")
    public ResponseEntity<JsonNode> getPopularVariety(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularVariety(page));
    }

//    熱門紀錄片
    @GetMapping("/popular-documentary")
    public ResponseEntity<JsonNode> getPopularComedy(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularDocumentary(page));
    }

//    熱門兒童節目
    @GetMapping("/popular-children")
    public ResponseEntity<JsonNode> getPopularChildren(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularChildren(page));
    }

//    熱門脫口秀
    @GetMapping("/popular-talkShow")
    public ResponseEntity<JsonNode> getPopularTalkShow(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularTalkShow(page));
    }

//    熱門人物
    @GetMapping("/popular-person")
    public ResponseEntity<JsonNode> getPopularPerson(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getPopularPerson(page));
    }

//    即將上映
    @GetMapping("/upcoming")
    public ResponseEntity<JsonNode> getUpcomingMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getUpcomingMovies(page));
    }

//    預告片
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

//    現正熱映
    @GetMapping("/now-playing")
    public ResponseEntity<JsonNode> getNowPlayingMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getNowPlayingMovies(page));
    }

//    好評推薦
    @GetMapping("/top-rated")
    public ResponseEntity<JsonNode> getTopRatedMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return ResponseEntity.ok(tmdbDiscoverService.getTopRatedMovies(page));
    }

//    電影詳情
    @GetMapping("/movie/{id}")
    public ResponseEntity<JsonNode> getMovieDetail(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbDetailService.getMovieDetail(id));
    }

//    電視節目詳情
    @GetMapping("/tv/{id}")
    public ResponseEntity<JsonNode> getTvDetail(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbDetailService.getTvDetail(id));
    }

//    人物詳情
    @GetMapping("/person/{id}")
    public ResponseEntity<JsonNode> getPersonDetail(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbDetailService.getPersonDetail(id));
    }
}
