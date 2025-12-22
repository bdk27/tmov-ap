package com.brian.tmov.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface TmdbDiscoverService {

//    取得一張隨機的電影背景圖(首頁)
    Map<String, String> getRandomPopularBackdrops(String category);

//    本日趨勢列表
    JsonNode getTrendingAll(String timeWindow, Integer page);

//    熱門電影列表
    JsonNode getPopularMovies(Integer page);

//    熱門電視節目
    JsonNode getPopularTv(Integer page);

//    熱門動畫列表
    JsonNode getPopularAnimation(Integer page);

//    熱門電視劇
    JsonNode getPopularDrama(Integer page);

//    熱門綜藝列表
    JsonNode getPopularVariety(Integer page);

//    熱門紀錄片
    JsonNode getPopularDocumentary(Integer page);

//    熱門兒童節目
    JsonNode getPopularChildren(Integer page);

//    熱門脫口秀
    JsonNode getPopularTalkShow(Integer page);

//    即將上映
    JsonNode getUpcomingMovies(Integer page);

//    現正熱映
    JsonNode getNowPlayingMovies(Integer page);

//    熱門人物列表
    JsonNode getPopularPerson(Integer page);

//    好評推薦
    JsonNode getTopRatedMovies(Integer page);

    JsonNode getTrendingMovies(String timeWindow);

//    最新預告片
    String getMovieTrailer(long movieId);
}
