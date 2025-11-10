package com.brian.tmov.service;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface TmdbDiscoverService {

//    取得一張隨機的熱門電影背景圖 URL
    Mono<Map<String, String>> getRandomPopularBackdropUrl();
}
