package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.exception.DownstreamException;
import com.brian.tmov.service.TmdbDetailService;
import com.brian.tmov.service.TmdbGetImageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class TmdbDetailServiceImpl implements TmdbDetailService {

    private static final Logger log = LoggerFactory.getLogger(TmdbDetailServiceImpl.class);

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private TmdbGetImageService tmdbGetImageService;

    @Value("${tmdb.default-language:zh-TW}")
    String defaultLanguage;

//    電影詳情
    @Override
    public Mono<JsonNode> getMovieDetail(long movieId) {
        return fetchDetail("movie", movieId, "credits,videos,recommendations,images");
    }

//    電視節目詳情
    @Override
    public Mono<JsonNode> getTvDetail(long tvId) {
        return fetchDetail("tv", tvId, "credits,videos,recommendations,images");
    }

//    人物詳情
    @Override
    public Mono<JsonNode> getPersonDetail(long personId) {
        return fetchDetail("person", personId, "combined_credits,images");
    }

    // ===================================================================================
    // 通用輔助方法
    // ===================================================================================

    private Mono<JsonNode> fetchDetail(String type, long id, String appendToResponse) {
        Map<String, String> qp = new HashMap<>();
        qp.put("language", defaultLanguage);

        // 關鍵參數：讓 TMDB 一次吐回所有關聯資料
        if (appendToResponse != null) {
            qp.put("append_to_response", appendToResponse);
            // 當我們請求 videos 時，希望包含中文和英文
            if (appendToResponse.contains("videos")) {
                qp.put("include_video_language", "zh,en");
            }
        }

        return tmdbClient.get(new String[]{type, String.valueOf(id)}, qp)
                .map(json -> transformDetailJson(json, type)) // 加工圖片網址
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 404) {
                        return Mono.error(new DownstreamException("找不到該項目 (404): " + type + "/" + id, e));
                    }
                    log.error("TMDB 詳情請求失敗 {}/{}: {}", type, id, e.getMessage());
                    return Mono.error(new DownstreamException("TMDB API 請求失敗", e));
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException), e -> {
                    log.error("詳情服務發生未知錯誤", e);
                    return Mono.error(new DownstreamException("TMDB 客戶端發生未知錯誤", e));
                });
    }

    /**
     * 深度加工 JSON，將所有相對路徑轉為絕對 URL
     * (處理根節點、演員列表、推薦列表、人物作品等)
     */
    private JsonNode transformDetailJson(JsonNode rootNode, String type) {
        if (rootNode == null || !rootNode.isObject()) return rootNode;
        ObjectNode root = (ObjectNode) rootNode;

        // 處理根節點圖片 (海報、背景、頭像)
        processImageField(root, "poster_path", "full_poster_url", "poster");
        processImageField(root, "backdrop_path", "full_backdrop_url", "backdrop");
        processImageField(root, "profile_path", "full_profile_url", "profile");

        // 處理演員列表 (credits.cast)
        if (root.has("credits") && root.get("credits").has("cast")) {
            for (JsonNode cast : root.get("credits").get("cast")) {
                if (cast.isObject()) {
                    processImageField((ObjectNode) cast, "profile_path", "full_profile_url", "profile");
                }
            }
        }

        // 處理推薦列表 (recommendations.results)
        if (root.has("recommendations") && root.get("recommendations").has("results")) {
            for (JsonNode rec : root.get("recommendations").get("results")) {
                if (rec.isObject()) {
                    processImageField((ObjectNode) rec, "poster_path", "full_poster_url", "poster");
                    processImageField((ObjectNode) rec, "backdrop_path", "full_backdrop_url", "backdrop");
                }
            }
        }

        // 處理人物的出演作品 (combined_credits.cast)
        if ("person".equals(type) && root.has("combined_credits") && root.get("combined_credits").has("cast")) {
            for (JsonNode credit : root.get("combined_credits").get("cast")) {
                if (credit.isObject()) {
                    processImageField((ObjectNode) credit, "poster_path", "full_poster_url", "poster");
                    processImageField((ObjectNode) credit, "backdrop_path", "full_backdrop_url", "backdrop");
                }
            }
        }

        return rootNode;
    }

    /**
     * 輔助：將相對路徑轉換並寫入新欄位
     */
    private void processImageField(ObjectNode node, String pathField, String urlField, String type) {
        if (node.has(pathField) && !node.get(pathField).isNull()) {
            String path = node.get(pathField).asText();
            String fullUrl = switch (type) {
                case "poster" -> tmdbGetImageService.getDefaultPosterUrl(path);
                case "backdrop" -> tmdbGetImageService.getDefaultBackdropUrl(path);
                case "profile" -> tmdbGetImageService.getDefaultProfileUrl(path);
                default -> null;
            };
            node.put(urlField, fullUrl);
        }
    }
}
