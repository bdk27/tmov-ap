package com.brian.tmov.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface TmdbDiscoverService {

//    取得一張隨機的電影背景圖(首頁)
    Mono<Map<String, String>> getRandomPopularBackdrops(String category);

//    本日趨勢列表
    Mono<JsonNode> getTrendingAll(String timeWindow, Integer page);

//    熱門電影列表
    Mono<JsonNode> getPopularMovies(Integer page);

//    熱門影集列表
    Mono<JsonNode> getPopularTv(Integer page);

//    熱門動畫列表
    Mono<JsonNode> getPopularAnimation(Integer page);

//    熱門綜藝列表
    Mono<JsonNode> getPopularVariety(Integer page);

//    即將上映
    Mono<JsonNode> getUpcomingMovies(Integer page);

//    現正熱映
    Mono<JsonNode> getNowPlayingMovies(Integer page);

//    熱門人物列表
    Mono<JsonNode> getPopularPerson(Integer page);


    Mono<JsonNode> getTrendingMovies(String timeWindow);

//    最新預告片
    Mono<String> getMovieTrailer(long movieId);
}
