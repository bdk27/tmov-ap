package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.service.TmdbDiscoverService;
import com.brian.tmov.service.TmdbGetImageService;
import com.brian.tmov.service.TmdbResponseTransformerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class TmdbDiscoverServiceImpl implements TmdbDiscoverService {

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
    public Map<String, String> getRandomPopularBackdrops(String category) {
        JsonNode responseNode = fetchSourceListByCategory(category);
        JsonNode results = responseNode.path("results");

        if (!results.isArray() || results.isEmpty()) {
            throw new IllegalArgumentException("無法從 TMDB 取得列表資料");
        }

        List<JsonNode> validItems = StreamSupport.stream(results.spliterator(), false)
                .filter(item -> {
                    String path = item.path("backdrop_path").asText(null);
                    return path != null && !path.isBlank() && !"null".equals(path);
                })
                .toList();

        if (validItems.isEmpty()) {
            throw new IllegalArgumentException("熱門列表中沒有任何項目包含背景圖");
        }

        // 隨機挑選
        JsonNode randomItem = validItems.get(random.nextInt(validItems.size()));
        String backdropPath = randomItem.path("backdrop_path").asText();
        long id = randomItem.path("id").asLong();

        // 判斷媒體類型以抓取預告片
        String mediaType = switch (category.toLowerCase()) {
            case "movie" -> "movie";
            default -> "tv";
        };

        // 使用虛擬執行緒並行抓取 (圖片 + 預告片)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // 任務 A: 組合圖片 URL
            Future<Map<String, String>> imagesFuture = executor.submit(() -> getBackdropUrls(backdropPath));

            // 任務 B: 抓取預告片
            Future<String> trailerFuture = executor.submit(() -> {
                try {
                    return fetchTrailerInternal(id, mediaType);
                } catch (Exception e) {
                    log.warn("Hero 區塊抓取預告片失敗 (非致命錯誤): {}", e.getMessage());
                    return null;
                }
            });

            // 等待結果
            Map<String, String> finalUrls = imagesFuture.get();
            String trailerUrl = trailerFuture.get();

            finalUrls.put("trailerUrl", trailerUrl); // 即使是 null 也放入
            return finalUrls;

        } catch (Exception e) {
            throw new RuntimeException("處理背景圖與預告片時發生錯誤", e);
        }
    }

    private JsonNode fetchSourceListByCategory(String category) {
        return switch (category.toLowerCase()) {
            case "tv" -> getPopularTv(1);
            case "anime" -> getPopularAnimation(1);
            case "variety" -> getPopularVariety(1);
            case "drama" -> getPopularDrama(1);
            default -> getPopularMovies(1);
        };
    }

    // ===================================================================================
    // 首頁五大分類
    // ===================================================================================

//    今日/本週趨勢
    @Override
    public JsonNode getTrendingAll(String timeWindow, Integer page) {
        String finalTimeWindow = (timeWindow != null && timeWindow.equals("week")) ? "week" : "day";
        JsonNode result = fetchListFromTmdb(new String[]{"trending", "all", finalTimeWindow}, Map.of("page", String.valueOf(page)));
        return filterOutPerson(result);
    }

//    熱門電影
    @Override
    public JsonNode getPopularMovies(Integer page) {
        return fetchPopularMoviePage(page);
    }

//    熱門電視節目
    @Override
    public JsonNode getPopularTv(Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("sort_by", "popularity.desc");
        params.put("without_genres", "16,10764"); // 排除動畫與綜藝

        JsonNode result = fetchListFromTmdb(new String[]{"discover", "tv"}, params);
        return filterOutGenres(result, Set.of(16, 10764));
    }

//    熱門動畫
    @Override
    public JsonNode getPopularAnimation(Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("with_genres", "16");
        params.put("sort_by", "popularity.desc");
        params.put("with_original_language", "ja|en|zh");
        return fetchListFromTmdb(new String[]{"discover", "tv"}, params);
    }

//    熱門電視劇
    @Override
    public JsonNode getPopularDrama(Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("with_genres", "18");
        params.put("without_genres", "16,10764");
        params.put("sort_by", "popularity.desc");
        params.put("with_original_language", "zh|ko|ja|en");

        JsonNode result = fetchListFromTmdb(new String[]{"discover", "tv"}, params);
        return filterOutGenres(result, Set.of(16, 10764));
    }

//    熱門綜藝
    @Override
    public JsonNode getPopularVariety(Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("with_genres", "10764");
        params.put("sort_by", "popularity.desc");
        params.put("with_original_language", "zh|ko|ja");
        return fetchListFromTmdb(new String[]{"discover", "tv"}, params);
    }

//    熱門紀錄片
    @Override
    public JsonNode getPopularDocumentary(Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("with_genres", "99");
        params.put("sort_by", "popularity.desc");
        return fetchListFromTmdb(new String[]{"discover", "tv"}, params);
    }

//    熱門兒童節目
    @Override
    public JsonNode getPopularChildren(Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("with_genres", "10762");
        return fetchListFromTmdb(new String[]{"discover", "tv"}, params);
    }

//    熱門脫口秀
    @Override
    public JsonNode getPopularTalkShow(Integer page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("with_genres", "10767");
        params.put("sort_by", "popularity.desc");
        return fetchListFromTmdb(new String[]{"discover", "tv"}, params);
    }

    //    即將上映
    @Override
    public JsonNode getUpcomingMovies(Integer page) {
        return fetchListFromTmdb(new String[]{"movie", "upcoming"}, Map.of("page", String.valueOf(page), "region", "TW"));
    }

//    現正熱映
    @Override
    public JsonNode getNowPlayingMovies(Integer page) {
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.minusDays(45);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("region", "TW");
        params.put("sort_by", "popularity.desc");
        params.put("release_date.gte", startDate.format(formatter));
        params.put("release_date.lte", now.format(formatter));
        params.put("with_release_type", "2|3"); // 2: 有限上映, 3: 院線上映

        return fetchListFromTmdb(new String[]{"discover", "movie"}, params);
    }

//    好評推薦
    public JsonNode getTopRatedMovies(Integer page) {
        return fetchListFromTmdb(new String[]{"movie", "top_rated"}, Map.of("page", String.valueOf(page), "region", "TW"));
    }

    //    熱門人物
    @Override
    public JsonNode getPopularPerson(Integer page) {
        return fetchListFromTmdb(new String[]{"person", "popular"}, Map.of("page", String.valueOf(page)));
    }

//    預告片
    @Override
    public JsonNode getTrendingMovies(String timeWindow) {
        String finalTimeWindow = (timeWindow != null && timeWindow.equals("week")) ? "week" : "day";
        return fetchListFromTmdb(new String[]{"trending", "movie", finalTimeWindow}, Collections.emptyMap());
    }

    //    最新預告片
    @Override
    public String getMovieTrailer(long movieId) {
        return fetchTrailerInternal(movieId, "movie");
    }

    // ===================================================================================
    // 通用輔助方法
    // ===================================================================================

    private String fetchTrailerInternal(long id, String endpointPrefix) {
        if (id == 0) return null;

        Map<String, String> qp = new HashMap<>();
        qp.put("language", defaultLanguage);
        qp.put("include_video_language", "zh,en");

        try {
            JsonNode videoNode = tmdbClient.get(new String[]{endpointPrefix, String.valueOf(id), "videos"}, qp);
            return findTrailerUrl(videoNode);
        } catch (Exception e) {
            log.warn("取得預告片失敗: {}", e.getMessage());
            return null; // 同步方法直接回傳 null
        }
    }

    private JsonNode fetchListFromTmdb(String[] path, Map<String, String> extraParams) {
        Map<String, String> qp = new HashMap<>();
        qp.put("language", defaultLanguage);
        qp.put("include_adult", "false");
        qp.putAll(extraParams);

        JsonNode result = tmdbClient.get(path, qp);
        return tmdbResponseTransformerService.transformSearchResponse(result);
    }

    private JsonNode fetchPopularMoviePage(Integer page) {
        String pageStr = String.valueOf(page == null || page < 1 ? 1 : page);
        // 使用 movie/popular 端點
        JsonNode result = tmdbClient.get(new String[]{"movie", "popular"},
                Map.of("language", defaultLanguage, "page", pageStr));

        return tmdbResponseTransformerService.transformSearchResponse(result);
    }

    private JsonNode filterOutGenres(JsonNode rootNode, Set<Integer> bannedIds) {
        if (rootNode.has("results") && rootNode.get("results").isArray()) {
            ArrayNode results = (ArrayNode) rootNode.get("results");
            Iterator<JsonNode> iterator = results.iterator();
            while (iterator.hasNext()) {
                JsonNode item = iterator.next();
                JsonNode genreIds = item.get("genre_ids");
                boolean shouldRemove = false;
                if (genreIds != null && genreIds.isArray()) {
                    for (JsonNode idNode : genreIds) {
                        if (bannedIds.contains(idNode.asInt())) {
                            shouldRemove = true;
                            break;
                        }
                    }
                }
                if (shouldRemove) iterator.remove();
            }
        }
        return rootNode;
    }

    private JsonNode filterOutPerson(JsonNode rootNode) {
        if (rootNode.has("results") && rootNode.get("results").isArray()) {
            ArrayNode results = (ArrayNode) rootNode.get("results");
            Iterator<JsonNode> iterator = results.iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                if ("person".equals(node.path("media_type").asText())) {
                    iterator.remove();
                }
            }
        }
        return rootNode;
    }

    private String findTrailerUrl(JsonNode videoNode) {
        JsonNode results = videoNode.path("results");
        if (!results.isArray() || results.isEmpty()) return null;
        String fallbackTrailer = null;
        String chineseTrailer = null;
        for (JsonNode video : results) {
            if ("YouTube".equals(video.path("site").asText())) {
                String key = video.path("key").asText(null);
                if (key == null) continue;
                String url = "https://www.youtube.com/embed/" + key;
                String type = video.path("type").asText();
                if ("Trailer".equals(type)) {
                    if ("zh".equals(video.path("iso_639_1").asText(""))) {
                        chineseTrailer = url;
                        break;
                    }
                    if (fallbackTrailer == null) fallbackTrailer = url;
                }
                if ("Teaser".equals(type) && fallbackTrailer == null) {
                    fallbackTrailer = url;
                }
            }
        }
        return (chineseTrailer != null) ? chineseTrailer : fallbackTrailer;
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