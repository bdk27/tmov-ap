package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.service.TmdbDetailService;
import com.brian.tmov.service.TmdbGetImageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TmdbDetailServiceImpl implements TmdbDetailService {

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private TmdbGetImageService tmdbGetImageService;

    @Value("${tmdb.default-language:zh-TW}")
    String defaultLanguage;

//    電影詳情
    @Override
    public JsonNode getMovieDetail(long movieId) {
        return fetchDetail("movie", movieId, "credits,videos,recommendations,images,release_dates,watch/providers,external_ids");
    }

//    電視節目詳情
    @Override
    public JsonNode getTvDetail(long tvId) {
        return fetchDetail("tv", tvId, "credits,videos,recommendations,images,content_ratings,watch/providers,external_ids");
    }

//    人物詳情
    @Override
    public JsonNode getPersonDetail(long personId) {
        return fetchDetail("person", personId, "combined_credits,images,translations,external_ids");
    }

    // ===================================================================================
    // 通用輔助方法
    // ===================================================================================

    private JsonNode fetchDetail(String type, long id, String appendToResponse) {
        Map<String, String> qp = new HashMap<>();
        qp.put("language", defaultLanguage);

        if (appendToResponse != null) {
            qp.put("append_to_response", appendToResponse);
            if (appendToResponse.contains("videos")) {
                qp.put("include_video_language", "zh,en");
            }
        }

        // 關鍵參數：讓 TMDB 一次吐回所有關聯資料
        if (appendToResponse != null) {
            qp.put("append_to_response", appendToResponse);
            // 當我們請求 videos 時，希望包含中文和英文
            if (appendToResponse.contains("videos")) {
                qp.put("include_video_language", "zh,en");
            }
        }

        JsonNode json = tmdbClient.get(new String[]{type, String.valueOf(id)}, qp);

        return transformDetailJson(json, type);
    }


//    深度加工 JSON，將所有相對路徑轉為絕對 URL
//    (處理根節點、演員列表、推薦列表、人物作品等)
    private JsonNode transformDetailJson(JsonNode rootNode, String type) {
        if (rootNode == null || !rootNode.isObject()) return rootNode;
        ObjectNode root = (ObjectNode) rootNode;

        // 處理人物傳記 (英文 fallback)
        if ("person".equals(type)) {
            String bio = root.path("biography").asText("");
            if (bio.isBlank()) {
                String enBio = findEnglishBiography(root.path("translations"));
                if (enBio != null && !enBio.isBlank()) {
                    root.put("biography", enBio);
                }
            }
        }

        // 1. 圖片處理 (基本)
        processImageField(root, "poster_path", "full_poster_url", "poster");
        processImageField(root, "backdrop_path", "full_backdrop_url", "backdrop");
        processImageField(root, "profile_path", "full_profile_url", "profile");

        // 2. 圖片處理 (關聯列表)
        if (root.has("credits") && root.get("credits").has("cast")) {
            for (JsonNode cast : root.get("credits").get("cast")) {
                if (cast.isObject()) processImageField((ObjectNode) cast, "profile_path", "full_profile_url", "profile");
            }
        }
        if (root.has("recommendations") && root.get("recommendations").has("results")) {
            for (JsonNode rec : root.get("recommendations").get("results")) {
                if (rec.isObject()) {
                    processImageField((ObjectNode) rec, "poster_path", "full_poster_url", "poster");
                    processImageField((ObjectNode) rec, "backdrop_path", "full_backdrop_url", "backdrop");
                }
            }
        }
        if ("person".equals(type) && root.has("combined_credits") && root.get("combined_credits").has("cast")) {
            for (JsonNode credit : root.get("combined_credits").get("cast")) {
                if (credit.isObject()) {
                    processImageField((ObjectNode) credit, "poster_path", "full_poster_url", "poster");
                    processImageField((ObjectNode) credit, "backdrop_path", "full_backdrop_url", "backdrop");
                }
            }
        }

        // 提取並簡化「分級資訊」
        String rating = "N/A";
        if ("movie".equals(type)) {
            rating = extractMovieCertification(root.path("release_dates"));
        } else if ("tv".equals(type)) {
            rating = extractTvContentRating(root.path("content_ratings"));
        }
        root.put("custom_rating", rating);

        // 提取並簡化「串流平台資訊」
        JsonNode watchProviders = extractWatchProviders(root.path("watch/providers"));
        if (watchProviders != null) {
            root.set("custom_watch_providers", watchProviders);
        }

        return rootNode;
    }


//    從翻譯節點中尋找英文傳記
    private String findEnglishBiography(JsonNode translationsNode) {
        JsonNode list = translationsNode.path("translations");
        if (list.isArray()) {
            for (JsonNode t : list) {
                if ("en".equals(t.path("iso_639_1").asText(""))) {
                    return t.path("data").path("biography").asText(null);
                }
            }
        }
        return null;
    }

//    提取電影分級 (優先找 TW，其次找 US)
    private String extractMovieCertification(JsonNode releaseDatesNode) {
        JsonNode results = releaseDatesNode.path("results");
        String usRating = null;

        if (results.isArray()) {
            for (JsonNode region : results) {
                String iso = region.path("iso_3166_1").asText("");

                if ("TW".equals(iso)) {
                    for (JsonNode release : region.path("release_dates")) {
                        String cert = release.path("certification").asText("");
                        if (!cert.isBlank()) return cert;
                    }
                }

                if ("US".equals(iso) && usRating == null) {
                    for (JsonNode release : region.path("release_dates")) {
                        String cert = release.path("certification").asText("");
                        if (!cert.isBlank()) {
                            usRating = cert;
                            break;
                        }
                    }
                }
            }
        }
        return usRating != null ? usRating : "N/A";
    }


//    提取 TV 分級 (優先找 TW，其次找 US)
    private String extractTvContentRating(JsonNode contentRatingsNode) {
        JsonNode results = contentRatingsNode.path("results");
        String usRating = null;

        if (results.isArray()) {
            for (JsonNode region : results) {
                String iso = region.path("iso_3166_1").asText("");
                String rating = region.path("rating").asText("");

                if ("TW".equals(iso) && !rating.isBlank()) {
                    return rating;
                }
                if ("US".equals(iso) && !rating.isBlank() && usRating == null) {
                    usRating = rating;
                }
            }
        }
        return usRating != null ? usRating : "N/A";
    }


//    提取 TW 地區的串流平台，並將 Logo 轉為完整網址
    private JsonNode extractWatchProviders(JsonNode providersNode) {
        JsonNode twProviders = providersNode.path("results").path("TW");

        if (twProviders.isMissingNode()) {
            return null;
        }

        ObjectNode result = (ObjectNode) twProviders.deepCopy();

        processProviderList(result, "flatrate");
        processProviderList(result, "rent");
        processProviderList(result, "buy");

        return result;
    }

    private void processProviderList(ObjectNode regionNode, String listKey) {
        if (regionNode.has(listKey) && regionNode.get(listKey).isArray()) {
            ArrayNode list = (ArrayNode) regionNode.get(listKey);
            for (JsonNode item : list) {
                if (item.isObject()) {
                    String logoPath = item.path("logo_path").asText(null);
                    String fullLogoUrl = tmdbGetImageService.getFullImageUrl(logoPath, "original");
                    ((ObjectNode) item).put("full_logo_url", fullLogoUrl);
                }
            }
        }
    }


//    將相對路徑轉換並寫入新欄位
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
