package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.service.TmdbGetImageService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class TmdbGetImageServiceImpl implements TmdbGetImageService {

    private static final Logger log = LoggerFactory.getLogger(TmdbGetImageServiceImpl.class);

    @Autowired
    private TmdbClient tmdbClient;

    // 我們將快取 TMDB 回傳的圖片基底 URL (例如 "https://image.tmdb.org/t/p/")
    private String imageBaseUrl;

    // 定義一些好用的預設尺寸
    private static final String DEFAULT_POSTER_SIZE = "w500";
    private static final String DEFAULT_BACKDROP_SIZE = "w1280";
    private static final String DEFAULT_PROFILE_SIZE = "h632";

    @PostConstruct
    public void init() {
        log.info("正在初始化 TmdbGetImageService，準備呼叫 TMDB /configuration API...");

        try {
            // .block() 會在 WebFlux 啟動時同步等待
            // 因為我們的應用程式在沒有這個 URL 之前不應該啟動
            JsonNode config = tmdbClient.get(new String[]{"configuration"}, Collections.emptyMap())
                    .block();

            if (config != null) {
                this.imageBaseUrl = config.path("images").path("secure_base_url").asText();
                log.info("TMDB 圖片基底 URL 已設定為: {}", this.imageBaseUrl);
            } else {
                log.error("無法取得 TMDB configuration, 回應為 null");
            }
        } catch (Exception e) {
            // 如果 API 呼叫失敗，應用程式日誌會記錄嚴重錯誤
            log.error("!!!!!!!!!!!! 嚴重：無法在啟動時取得 TMDB configuration !!!!!!!!!!!!", e);
        }
    }

    @Override
    public String getFullImageUrl(String path, String size) {
        // 如果 imageBaseUrl 尚未準備好，或 path 是空的 (例如某電影沒有海報)
        if (this.imageBaseUrl == null || path == null || path.isBlank() || path.equals("null")) {
            return null; // 回傳 null (前端會顯示一個預設圖片)
        }
        return this.imageBaseUrl + size + path;
    }

    @Override
    public String getDefaultPosterUrl(String posterPath) {
        return getFullImageUrl(posterPath, DEFAULT_POSTER_SIZE);
    }

    @Override
    public String getDefaultBackdropUrl(String backdropPath) {
        return getFullImageUrl(backdropPath, DEFAULT_BACKDROP_SIZE);
    }

    @Override
    public String getDefaultProfileUrl(String profilePath) {
        return getFullImageUrl(profilePath, DEFAULT_PROFILE_SIZE);
    }
}