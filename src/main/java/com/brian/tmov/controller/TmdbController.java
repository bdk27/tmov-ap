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

    @GetMapping("/search")
    public Mono<ResponseEntity<JsonNode>> search(@Valid TmdbSearchQuery query) {
        return tmdbSearchService.search(query)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/popular-backdrop")
    public Mono<ResponseEntity<Map<String, String>>> getRandomPopularBackdrop() {
        return tmdbDiscoverService.getRandomPopularBackdrops()
                .map(urls -> {
                    return ResponseEntity.ok().body(urls);
                });
    }

    @GetMapping("/popular-movies")
    public Mono<ResponseEntity<JsonNode>> getPopularMovies() {
        return tmdbDiscoverService.getPopularMovies()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/popular-tv")
    public Mono<ResponseEntity<JsonNode>> getPopularTv() {
        return tmdbDiscoverService.getPopularTv()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/popular-person")
    public Mono<ResponseEntity<JsonNode>> getPopularPerson() {
        return tmdbDiscoverService.getPopularPerson()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/trending")
    public Mono<ResponseEntity<JsonNode>> getTrendingMovies(
            @RequestParam(value = "time_window", defaultValue = "day") String timeWindow) {
        return tmdbDiscoverService.getTrendingMovies(timeWindow)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/upcoming")
    public Mono<ResponseEntity<JsonNode>> getUpcomingMovies() {
        return tmdbDiscoverService.getUpcomingMovies().
                map(ResponseEntity::ok);
    }

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

    @GetMapping("/now-playing")
    public Mono<ResponseEntity<JsonNode>> getNowPlayingMovies() {
        return tmdbDiscoverService.getNowPlayingMovies().
                map(ResponseEntity::ok);
    }
}
