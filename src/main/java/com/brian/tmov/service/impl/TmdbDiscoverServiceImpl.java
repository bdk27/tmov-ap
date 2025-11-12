package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.exception.DownstreamException;
import com.brian.tmov.service.TmdbDiscoverService;
import com.brian.tmov.service.TmdbGetImageService;
import com.brian.tmov.service.TmdbResponseTransformerService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class TmdbDiscoverServiceImpl implements TmdbDiscoverService {

    private static final Logger log = LoggerFactory.getLogger(TmdbDiscoverServiceImpl.class);

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private TmdbGetImageService tmdbGetImageService;

    @Autowired
    private TmdbResponseTransformerService tmdbResponseTransformerService;

    @Value("${tmdb.default-language:zh-TW}")
    String defaultLanguage;

    private final Random random = new Random();

    @Override
    public Mono<Map<String, String>> getRandomPopularBackdrops() {
        // 1. 呼叫私有輔助方法，取得第一頁的熱門電影
        return this.fetchPopularMoviePage()
                // 2. 使用 flatMap 進行鏈式呼叫 (處理非同步)
                .flatMap(responseNode -> {
                    JsonNode results = responseNode.path("results");
                    if (results.isArray() && !results.isEmpty()) {
                        // 3. 隨機挑選一部電影
                        JsonNode randomMovie = results.get(random.nextInt(results.size()));
                        String backdropPath = randomMovie.path("backdrop_path").asText(null);
                        long movieId = randomMovie.path("id").asLong();

                        // 4. (重要) 使用 Mono.zip 並行取得圖片和預告片
                        //    Mono.zip 會等待兩個 Mono 都完成
                        return Mono.zip(
                                // Mono 1: 取得圖片 (這是同步的，所以用 just 包裝)
                                Mono.just(getBackdropUrls(backdropPath)),
                                // Mono 2: 取得預告片 (這是非同步的)
                                this.fetchMovieTrailer(movieId)
                        );
                    }
                    return Mono.error(new IllegalArgumentException("無法從 TMDB 取得熱門電影列表"));
                })
                // 5. 組合最終結果
                .map(tuple -> {
                    // tuple.getT1() 是圖片 Map
                    // tuple.getT2() 是預告片 URL (可能為 null)
                    Map<String, String> finalUrls = tuple.getT1();
                    finalUrls.put("trailerUrl", tuple.getT2());
                    return finalUrls;
                });
    }

    private Mono<JsonNode> fetchPopularMoviePage() {
        Map<String, String> qp = Map.of(
                "language", defaultLanguage,
                "page", "1"
        );
        return tmdbClient.get(new String[]{"movie", "popular"}, qp)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("TMDB /movie/popular API 請求失敗: {}", e.getMessage());
                    return Mono.error(new DownstreamException("TMDB API /movie/popular 請求失敗", e));
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException), e -> {
                    log.error("TMDB 客戶端未知錯誤 (fetchPopularMoviePage): {}", e.getMessage());
                    return Mono.error(new DownstreamException("TMDB 客戶端發生未知錯誤", e));
                });
    }

    private Mono<String> fetchMovieTrailer(long movieId) {
        if (movieId == 0) {
            return Mono.empty(); // 如果沒有 movieId，返回空的 Mono
        }

        // 取得預告片 (不需要語言，通常 EN 預告片最完整)
        return tmdbClient.get(new String[]{"movie", String.valueOf(movieId), "videos"}, Collections.emptyMap())
                // (修正) 使用 flatMap 和 Mono.justOrEmpty 來安全處理 null
                .flatMap(videoNode -> {
                    String trailerUrl = findTrailerUrl(videoNode);
                    return Mono.justOrEmpty(trailerUrl); // 如果 trailerUrl 是 null，會返回 Mono.empty()
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    // 404 Not Found 是很常見的 (例如電影沒有影片)，不應視為嚴重錯誤
                    if (e.getStatusCode().value() == 404) {
                        log.warn("無法取得電影 ID {} 的預告片 (404 Not Found)", movieId);
                        return Mono.empty(); // 返回空 Mono，而不是拋出錯誤
                    }
                    log.error("TMDB /movie/videos API 請求失敗: {}", e.getMessage());
                    return Mono.error(new DownstreamException("TMDB API /movie/videos 請求失敗", e));
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException), e -> {
                    log.error("TMDB 客戶端未知錯誤 (fetchMovieTrailer): {}", e.getMessage());
                    return Mono.error(new DownstreamException("TMDB 客戶端發生未知錯誤", e));
                });
    }

    private String findTrailerUrl(JsonNode videoNode) {
        JsonNode results = videoNode.path("results");
        if (results.isArray() && !results.isEmpty()) {
            for (JsonNode video : results) {
                // 必須是 "Trailer" 且 來源是 "YouTube"
                if ("Trailer".equals(video.path("type").asText()) &&
                        "YouTube".equals(video.path("site").asText())) {
                    String key = video.path("key").asText(null);
                    if (key != null) {
                        return "https://www.youtube.com/embed/" + key;
                    }
                }
            }
        }
        log.warn("在 videoNode 中找不到 'YouTube' 的 'Trailer'");
        return null; // 找不到預告片
    }

    private Map<String, String> getBackdropUrls(String backdropPath) {
        // (修正) 使用 w780 作為手機版背景圖
        String desktopUrl = tmdbGetImageService.getFullImageUrl(backdropPath, "original");
        String mobileUrl = tmdbGetImageService.getFullImageUrl(backdropPath, "w780");

        Map<String, String> urls = new HashMap<>();
        urls.put("backdropDesktopUrl", desktopUrl);
        urls.put("backdropMobileUrl", mobileUrl);
        return urls;
    }

    @Override
    public Mono<JsonNode> getPopularMovies() {
        Map<String, String> qp = Map.of(
                "language", defaultLanguage,
                "page", "1"
        );

        return tmdbClient.get(new String[]{"movie", "popular"}, qp)
                .map(tmdbResponseTransformerService::transformSearchResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    String errorBody = e.getResponseBodyAsString();
                    log.warn("呼叫 TMDB /movie/popular (for list) 失敗: {}", errorBody, e);
                    return Mono.error(new DownstreamException("TMDB API 請求失敗: " + e.getStatusCode() + " " + errorBody, e));
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException || ex instanceof IllegalArgumentException), e -> {
                    log.error("處理 /movie/popular (for list) 時發生未知錯誤", e);
                    return Mono.error(new DownstreamException("TMDB 客戶端發生未知錯誤: " + e.getMessage(), e));
                });
    }

    @Override
    public Mono<JsonNode> getTrendingMovies() {
        Map<String, String> qp = Map.of(
                "language", defaultLanguage
        );

        // 呼叫 /trending/movie/day
        return tmdbClient.get(new String[]{"trending", "movie", "day"}, qp)
                // (關鍵) 同樣重複使用 Transformer 服務來加工 JSON
                .map(tmdbResponseTransformerService::transformSearchResponse)
                // --- 統一的錯誤處理 ---
                .onErrorResume(WebClientResponseException.class, e -> {
                    String errorBody = e.getResponseBodyAsString();
                    log.error("TMDB /trending/movie/day API 請求失敗 ({}): {}", e.getStatusCode(), errorBody, e);
                    return Mono.error(new DownstreamException("TMDB API 請求失敗: " + e.getStatusCode(), e));
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException || ex instanceof IllegalArgumentException), e -> {
                    log.error("TMDB 客戶端發生未知錯誤 (Trending)", e);
                    return Mono.error(new DownstreamException("TMDB 客戶端發生未知錯誤: " + e.getMessage(), e));
                });
    }
}
