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

    @Override
    public JsonNode transformSearchResponse(JsonNode responseNode) {
        // 檢查 "results" 欄位是否存在並且是一個陣列
        if (responseNode == null || !responseNode.has("results") || !responseNode.get("results").isArray()) {
            return responseNode; // 如果沒有 results 陣列 (例如 /configuration)，直接回傳
        }

        // 取得 "results" 陣列
        JsonNode results = responseNode.get("results");

        // 遍歷 (Loop) 陣列中的每一個項目
        for (JsonNode item : results) {
            // 我們需要將 JsonNode (不可變) 轉換為 ObjectNode (可變) 才能新增欄位
            if (item.isObject()) {
                ObjectNode objectItem = (ObjectNode) item;

                // 1. 取得 poster_path 並轉換
                String posterPath = objectItem.path("poster_path").asText(null);
                String fullPosterUrl = tmdbGetImageService.getDefaultPosterUrl(posterPath);
                objectItem.put("full_poster_url", fullPosterUrl);

                // 2. 取得 backdrop_path 並轉換
                String backdropPath = objectItem.path("backdrop_path").asText(null);
                String fullBackdropUrl = tmdbGetImageService.getDefaultBackdropUrl(backdropPath);
                objectItem.put("full_backdrop_url", fullBackdropUrl);

                // 3. (可選) 取得 profile_path 並轉換 (用於 "person" 類型的結果)
                String profilePath = objectItem.path("profile_path").asText(null);
                String fullProfileUrl = tmdbGetImageService.getDefaultProfileUrl(profilePath);
                objectItem.put("full_profile_url", fullProfileUrl);
            }
        }

        return responseNode;
    }
}
