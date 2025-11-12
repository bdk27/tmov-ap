package com.brian.tmov.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface TmdbDiscoverService {

//    取得一張隨機的電影背景圖
    Mono<Map<String, String>> getRandomPopularBackdrops();

    //    取得熱門電影列表
    Mono<JsonNode> getPopularMovies();

//    取得本日趨勢電影列表
    Mono<JsonNode> getTrendingMovies();
}
