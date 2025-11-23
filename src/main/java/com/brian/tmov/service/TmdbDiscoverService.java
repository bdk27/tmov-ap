package com.brian.tmov.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface TmdbDiscoverService {

//    取得一張隨機的電影背景圖
    Mono<Map<String, String>> getRandomPopularBackdrops();

//    熱門電影列表
    Mono<JsonNode> getPopularMovies();

//    熱門影集列表
    Mono<JsonNode> getPopularTv();

//    熱門人物列表
    Mono<JsonNode> getPopularPerson();

//    本日趨勢電影列表
    Mono<JsonNode> getTrendingMovies(String timeWindow);

//    即將上映列表
    Mono<JsonNode> getUpcomingMovies();

//    現正熱映列表
    Mono<JsonNode> getNowPlayingMovies();

    Mono<String> getMovieTrailer(long movieId);
}
