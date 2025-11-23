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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    // ===================================================================================
    // 取得隨機背景 (首頁 Hero)
    // ===================================================================================

    @Override
    public Mono<Map<String, String>> getRandomPopularBackdrops() {
        return this.fetchPopularMoviePage()
                .flatMap(responseNode -> {
                    JsonNode results = responseNode.path("results");
                    if (results.isArray() && !results.isEmpty()) {

                        // 過濾：只保留 "有背景圖" 的電影
                        List<JsonNode> validMovies = StreamSupport.stream(results.spliterator(), false)
                                .filter(movie -> {
                                    String path = movie.path("backdrop_path").asText(null);
                                    return path != null && !path.isBlank() && !"null".equals(path);
                                })
                                .toList();

                        if (validMovies.isEmpty()) {
                            return Mono.error(new IllegalArgumentException("熱門列表中沒有任何電影包含背景圖"));
                        }

                        // 隨機挑選
                        JsonNode randomMovie = validMovies.get(random.nextInt(validMovies.size()));

                        String backdropPath = randomMovie.path("backdrop_path").asText();
                        long movieId = randomMovie.path("id").asLong();

                        // 並行取得：(A) 圖片網址 + (B) 預告片
                        return Mono.zip(
                                Mono.just(getBackdropUrls(backdropPath)),
                                // 【關鍵修正】加上 .defaultIfEmpty("")
                                // 確保即使沒有預告片，Mono.zip 也不會崩潰，背景圖依然能顯示
                                this.getMovieTrailer(movieId).defaultIfEmpty("")
                        );
                    }
                    return Mono.error(new IllegalArgumentException("無法從 TMDB 取得熱門電影列表"));
                })
                .map(tuple -> {
                    Map<String, String> finalUrls = tuple.getT1();
                    String trailerUrl = tuple.getT2();

                    // 處理空字串轉 null
                    if (trailerUrl.isEmpty()) {
                        finalUrls.put("trailerUrl", null);
                    } else {
                        finalUrls.put("trailerUrl", trailerUrl);
                    }
                    return finalUrls;
                });
    }

    // ===================================================================================
    // 公開 API 實作
    // ===================================================================================

    @Override
    public Mono<JsonNode> getPopularMovies() {
        // 重用 fetchPopularMoviePage，再加上 JSON 轉換
        return this.fetchPopularMoviePage()
                .map(tmdbResponseTransformerService::transformSearchResponse);
    }

    @Override
    public Mono<JsonNode> getPopularTv() {
        return fetchListFromTmdb(new String[]{"tv", "popular"}, Map.of("page", "1"));
    }

    @Override
    public Mono<JsonNode> getPopularPerson() {
        return fetchListFromTmdb(new String[]{"person", "popular"}, Map.of("page", "1"));
    }

    @Override
    public Mono<JsonNode> getTrendingMovies(String timeWindow) {
        String finalTimeWindow = (timeWindow != null && timeWindow.equals("week")) ? "week" : "day";
        return fetchListFromTmdb(new String[]{"trending", "movie", finalTimeWindow}, Collections.emptyMap());
    }

    @Override
    public Mono<JsonNode> getUpcomingMovies() {
        return fetchListFromTmdb(new String[]{"movie", "upcoming"}, Map.of("page", "1", "region", "TW"));
    }

    @Override
    public Mono<JsonNode> getNowPlayingMovies() {
        return fetchListFromTmdb(new String[]{"movie", "now_playing"}, Map.of("page", "1", "region", "TW"));
    }

    @Override
    public Mono<String> getMovieTrailer(long movieId) {
        if (movieId == 0) return Mono.empty();

        Map<String, String> qp = new HashMap<>();
        qp.put("language", defaultLanguage);
        qp.put("include_video_language", "zh,en");

        return tmdbClient.get(new String[]{"movie", String.valueOf(movieId), "videos"}, qp)
                .flatMap(videoNode -> {
                    String trailerUrl = findTrailerUrl(videoNode);
                    return Mono.justOrEmpty(trailerUrl);
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 404) {
                        return Mono.empty();
                    }
                    log.error("TMDB /movie/{}/videos 請求失敗: {}", movieId, e.getMessage());
                    return Mono.error(new DownstreamException("無法取得預告片", e));
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException), e -> {
                    log.error("取得預告片時發生未知錯誤", e);
                    return Mono.empty();
                });
    }

    // ===================================================================================
    // 通用輔助方法
    // ===================================================================================

    private Mono<JsonNode> fetchListFromTmdb(String[] path, Map<String, String> extraParams) {
        Map<String, String> qp = new HashMap<>();
        qp.put("language", defaultLanguage);
        qp.putAll(extraParams);

        return tmdbClient.get(path, qp)
                .map(tmdbResponseTransformerService::transformSearchResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    String errorBody = e.getResponseBodyAsString();
                    return Mono.error(new DownstreamException("TMDB API 請求失敗: " + e.getStatusCode(), e));
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException), e -> {
                    return Mono.error(new DownstreamException("TMDB 客戶端發生未知錯誤", e));
                });
    }

    private Mono<JsonNode> fetchPopularMoviePage() {
        Map<String, String> qp = Map.of("language", defaultLanguage, "page", "1");
        return tmdbClient.get(new String[]{"movie", "popular"}, qp)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("TMDB /movie/popular 請求失敗: {}", e.getMessage());
                    return Mono.error(new DownstreamException("TMDB API 請求失敗", e));
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException), e -> {
                    return Mono.error(new DownstreamException("TMDB 未知錯誤", e));
                });
    }

    private String findTrailerUrl(JsonNode videoNode) {
        JsonNode results = videoNode.path("results");
        if (!results.isArray() || results.isEmpty()) return null;

        String fallback = null;

        for (JsonNode video : results) {
            // 只看 YouTube
            if ("YouTube".equals(video.path("site").asText())) {
                String key = video.path("key").asText(null);
                if (key == null) continue;

                String url = "https://www.youtube.com/embed/" + key; // 移除自動播放參數
                String type = video.path("type").asText();

                if ("Trailer".equals(type)) {
                    return url;
                }

                if ("Teaser".equals(type) && fallback == null) {
                    fallback = url;
                }
            }
        }

        return fallback;
    }

    private Map<String, String> getBackdropUrls(String backdropPath) {
        String desktopUrl = tmdbGetImageService.getFullImageUrl(backdropPath, "w1280");
        String mobileUrl = tmdbGetImageService.getFullImageUrl(backdropPath, "w780");
        Map<String, String> urls = new HashMap<>();
        urls.put("backdropDesktopUrl", desktopUrl);
        urls.put("backdropMobileUrl", mobileUrl);
        return urls;
    }
}