package com.brian.tmov.controller;

import com.brian.tmov.dto.TmdbSearchQuery;
import com.brian.tmov.service.TmdbDiscoverService;
import com.brian.tmov.service.TmdbSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/tmdb")
public class TmdbController {

    @Autowired
    private TmdbSearchService tmdbSearchService;

    @Autowired
    private TmdbDiscoverService tmdbDiscoverService;

//    搜尋
    @GetMapping("/search")
    public Mono<ResponseEntity<JsonNode>> search(@Valid TmdbSearchQuery query) {
        return tmdbSearchService.search(query).map(ResponseEntity::ok);
    }

//    隨機背景
    @GetMapping("/popular-backdrop")
    public Mono<ResponseEntity<Map<String, String>>> getRandomPopularBackdrop(
            @RequestParam(defaultValue = "movie") String category
    ) {
        return tmdbDiscoverService.getRandomPopularBackdrops(category).map(ResponseEntity::ok);
    }

//    趨勢
    @GetMapping("/trending")
    public Mono<ResponseEntity<JsonNode>> getTrendingMovies(
            @RequestParam(value = "time_window", defaultValue = "day") String timeWindow,
            @RequestParam(value = "page", defaultValue = "1") Integer page
   ) {
        return tmdbDiscoverService.getTrendingAll(timeWindow, page).map(ResponseEntity::ok);
    }

//    熱門電影
    @GetMapping("/popular-movies")
    public Mono<ResponseEntity<JsonNode>> getPopularMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return tmdbDiscoverService.getPopularMovies(page).map(ResponseEntity::ok);
    }

//    熱門電視劇
    @GetMapping("/popular-tv")
    public Mono<ResponseEntity<JsonNode>> getPopularTv(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return tmdbDiscoverService.getPopularTv(page).map(ResponseEntity::ok);
    }

//    熱門動畫
    @GetMapping("/popular-anime")
    public Mono<ResponseEntity<JsonNode>> getPopularAnimation(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return tmdbDiscoverService.getPopularAnimation(page).map(ResponseEntity::ok);
    }

//    熱門綜藝
    @GetMapping("/popular-variety")
    public Mono<ResponseEntity<JsonNode>> getPopularVariety(
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return tmdbDiscoverService.getPopularVariety(page).map(ResponseEntity::ok);
    }

//    熱門人物
    @GetMapping("/popular-person")
    public Mono<ResponseEntity<JsonNode>> getPopularPerson(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return tmdbDiscoverService.getPopularPerson(page).map(ResponseEntity::ok);
    }

//    即將上映
    @GetMapping("/upcoming")
    public Mono<ResponseEntity<JsonNode>> getUpcomingMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return tmdbDiscoverService.getUpcomingMovies(page).map(ResponseEntity::ok);
    }

//    預告片
    @GetMapping("/movie/{id}/trailer")
    public Mono<ResponseEntity<JsonNode>> getMovieTrailer(@PathVariable Long id) {
        return tmdbDiscoverService.getMovieTrailer(id)
                .map(url -> {
                    ObjectNode json = JsonNodeFactory.instance.objectNode();
                    json.put("trailerUrl", url);
                    return json;
                })
                .defaultIfEmpty(JsonNodeFactory.instance.objectNode().putNull("trailerUrl"))
                .map(json -> ResponseEntity.ok().body(json));
    }

//    現正熱映
    @GetMapping("/now-playing")
    public Mono<ResponseEntity<JsonNode>> getNowPlayingMovies(
            @RequestParam(value = "page", defaultValue = "1") Integer page
    ) {
        return tmdbDiscoverService.getNowPlayingMovies(page).map(ResponseEntity::ok);
    }
}
