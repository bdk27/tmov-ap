package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.exception.DownstreamException;
import com.brian.tmov.service.TmdbDiscoverService;
import com.brian.tmov.service.TmdbGetImageService;
import com.brian.tmov.service.TmdbResponseTransformerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;
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
    // 首頁 Hero (隨機背景)
    // ===================================================================================

    @Override
    public Mono<Map<String, String>> getRandomPopularBackdrops(String category) {
        Mono<JsonNode> sourceList;
        String mediaType = switch (category.toLowerCase()) {
            case "tv" -> {
                sourceList = getPopularTv(1);
                yield "tv";
            }
            case "anime" -> {
                sourceList = getPopularAnimation(1);
                yield "tv";
            }
            case "variety" -> {
                sourceList = getPopularVariety(1);
                yield "tv";
            }
            default -> {
                sourceList = getPopularMovies(1);
                yield "movie";
            }
        };

        return sourceList
                .flatMap(responseNode -> {
                    JsonNode results = responseNode.path("results");
                    if (results.isArray() && !results.isEmpty()) {

                        // 過濾：只保留 "有背景圖" 的項目
                        List<JsonNode> validItems = StreamSupport.stream(results.spliterator(), false)
                                .filter(item -> {
                                    String path = item.path("backdrop_path").asText(null);
                                    return path != null && !path.isBlank() && !"null".equals(path);
                                })
                                .toList();

                        if (validItems.isEmpty()) {
                            return Mono.error(new IllegalArgumentException("熱門列表中沒有任何項目包含背景圖"));
                        }

                        // 隨機挑選
                        JsonNode randomItem = validItems.get(random.nextInt(validItems.size()));

                        String backdropPath = randomItem.path("backdrop_path").asText();
                        long id = randomItem.path("id").asLong();

                        // 並行取得：(A) 圖片網址 + (B) 預告片 (根據 mediaType 抓取)
                        return Mono.zip(
                                Mono.just(getBackdropUrls(backdropPath)),
                                this.fetchTrailerInternal(id, mediaType).defaultIfEmpty("")
                        );
                    }
                    return Mono.error(new IllegalArgumentException("無法從 TMDB 取得列表資料"));
                })
                .map(tuple -> {
                    Map<String, String> finalUrls = tuple.getT1();
                    String trailerUrl = tuple.getT2();

                    if (trailerUrl.isEmpty()) {
                        finalUrls.put("trailerUrl", null);
                    } else {
                        finalUrls.put("trailerUrl", trailerUrl);
                    }
                    return finalUrls;
                });
    }

    // ===================================================================================
    // 首頁五大分類
    // ===================================================================================

//    今日/本週趨勢
    @Override
    public Mono<JsonNode> getTrendingAll(String timeWindow, Integer page) {
        String finalTimeWindow = (timeWindow != null && timeWindow.equals("week")) ? "week" : "day";
        return fetchListFromTmdb(new String[]{"trending", "movie", finalTimeWindow}, Map.of("page", String.valueOf(page)))
                .map(this::filterOutPerson);
    }

//    熱門電影
    @Override
    public Mono<JsonNode> getPopularMovies(Integer page) {
        return this.fetchPopularMoviePage(page)
                .map(tmdbResponseTransformerService::transformSearchResponse);
    }

//    熱門電視劇
    @Override
    public Mono<JsonNode> getPopularTv(Integer page) {
        return fetchListFromTmdb(new String[]{"tv", "popular"}, Map.of("page", String.valueOf(page)));
    }

//    熱門動畫
    @Override
    public Mono<JsonNode> getPopularAnimation(Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("with_genres", "16");
        params.put("sort_by", "popularity.desc");
        params.put("with_original_language", "ja|en|zh");
        return fetchListFromTmdb(new String[]{"discover", "tv"}, params);
    }

//    熱門綜藝
    @Override
    public Mono<JsonNode> getPopularVariety(Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("with_genres", "10764");
        params.put("sort_by", "popularity.desc");
        params.put("with_original_language", "zh|ko|ja");
        return fetchListFromTmdb(new String[]{"discover", "tv"}, params);
    }

//    即將上映
    @Override
    public Mono<JsonNode> getUpcomingMovies(Integer page) {
            return fetchListFromTmdb(new String[]{"movie", "upcoming"}, Map.of("page", String.valueOf(page), "region", "TW"));
        }

//    現正熱映
    @Override
    public Mono<JsonNode> getNowPlayingMovies(Integer page) {
        return fetchListFromTmdb(new String[]{"movie", "now_playing"}, Map.of("page", String.valueOf(page), "region", "TW"));
    }

//    熱門人物
    @Override
    public Mono<JsonNode> getPopularPerson(Integer page) {
        return fetchListFromTmdb(new String[]{"person", "popular"}, Map.of("page", String.valueOf(page)));
    }

//    預告片
    @Override
    public Mono<JsonNode> getTrendingMovies(String timeWindow) {
        String finalTimeWindow = (timeWindow != null && timeWindow.equals("week")) ? "week" : "day";
        return fetchListFromTmdb(new String[]{"trending", "movie", finalTimeWindow}, Collections.emptyMap());
    }

//    最新預告片
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

    private Mono<String> fetchTrailerInternal(long id, String endpointPrefix) {
        if (id == 0) return Mono.empty();

        Map<String, String> qp = new HashMap<>();
        qp.put("language", defaultLanguage);
        qp.put("include_video_language", "zh,en");

        return tmdbClient.get(new String[]{endpointPrefix, String.valueOf(id), "videos"}, qp)
                .flatMap(videoNode -> {
                    String trailerUrl = findTrailerUrl(videoNode);
                    return Mono.justOrEmpty(trailerUrl);
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 404) {
                        return Mono.empty();
                    }
                    log.error("TMDB /{}/{}/videos 請求失敗: {}", endpointPrefix, id, e.getMessage());
                    return Mono.empty(); // 預告片錯誤不應中斷主流程
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException), e -> {
                    log.error("取得預告片時發生未知錯誤", e);
                    return Mono.empty();
                });
    }

//    趨勢過濾人物
    private JsonNode filterOutPerson(JsonNode rootNode) {
        if (rootNode.has("results") && rootNode.get("results").isArray()) {
            ArrayNode results = (ArrayNode) rootNode.get("results");
            Iterator<JsonNode> iterator = results.iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                // 如果 media_type 是 "person"，就移除
                if ("person".equals(node.path("media_type").asText())) {
                    iterator.remove();
                }
            }
        }
        return rootNode;
    }

    private Mono<JsonNode> fetchListFromTmdb(String[] path, Map<String, String> extraParams) {
        Map<String, String> qp = new HashMap<>();
        qp.put("language", defaultLanguage);
        qp.put("include_adult", "false");
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

    private Mono<JsonNode> fetchPopularMoviePage(Integer page) {
        String pageStr = String.valueOf(page == null || page < 1 ? 1 : page);
        Map<String, String> qp = Map.of("language", defaultLanguage, "page", pageStr);

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