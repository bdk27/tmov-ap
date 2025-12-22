package com.brian.tmov.service.impl;

import com.brian.tmov.service.TmdbGetImageService;
import com.brian.tmov.service.TmdbResponseTransformerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TmdbResponseTransformerServiceImpl implements TmdbResponseTransformerService {

    @Autowired
    private TmdbGetImageService tmdbGetImageService;

    private static final int MAX_TMDB_PAGES = 500;

    @Override
    public JsonNode transformSearchResponse(JsonNode responseNode) {
        // 檢查基本結構
        if (responseNode == null) {
            return null;
        }

        // 限制 total_pages 上限
        // 防止前端分頁元件產生第 501 頁的按鈕，避免使用者點擊後導致 502 錯誤
        if (responseNode.has("total_pages") && responseNode instanceof ObjectNode) {
            int totalPages = responseNode.get("total_pages").asInt();
            if (totalPages > MAX_TMDB_PAGES) {
                ((ObjectNode) responseNode).put("total_pages", MAX_TMDB_PAGES);
            }
        }

        // 檢查 results 陣列
        if (!responseNode.has("results") || !responseNode.get("results").isArray()) {
            return responseNode;
        }

        // 取得 "results" 陣列
        JsonNode results = responseNode.get("results");

        // 2. 遍歷 (Loop) 陣列中的每一個項目並加工圖片
        for (JsonNode item : results) {
            if (item.isObject()) {
                ObjectNode objectItem = (ObjectNode) item;

                // 處理 poster_path
                String posterPath = objectItem.path("poster_path").asText(null);
                String fullPosterUrl = tmdbGetImageService.getDefaultPosterUrl(posterPath);
                objectItem.put("full_poster_url", fullPosterUrl);

                // 處理 backdrop_path
                String backdropPath = objectItem.path("backdrop_path").asText(null);
                String fullBackdropUrl = tmdbGetImageService.getDefaultBackdropUrl(backdropPath);
                objectItem.put("full_backdrop_url", fullBackdropUrl);

                // 處理 profile_path (用於 "person" 類型的結果)
                String profilePath = objectItem.path("profile_path").asText(null);
                String fullProfileUrl = tmdbGetImageService.getDefaultProfileUrl(profilePath);
                objectItem.put("full_profile_url", fullProfileUrl);
            }
        }

        return responseNode;
    }
}
