package com.brian.tmov.service;

import reactor.core.publisher.Mono;

public interface TmdbDiscoverService {

//    取得一張隨機的熱門電影背景圖 (w1280) URL
    Mono<String> getRandomPopularBackdropUrl();
}
