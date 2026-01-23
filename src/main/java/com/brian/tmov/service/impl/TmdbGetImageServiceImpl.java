package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.service.TmdbGetImageService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class TmdbGetImageServiceImpl implements TmdbGetImageService {

    @Autowired
    private TmdbClient tmdbClient;

    private String imageBaseUrl = "https://image.tmdb.org/t/p/";

    private static final String DEFAULT_POSTER_SIZE = "w780";
    private static final String DEFAULT_BACKDROP_SIZE = "w1280";
    private static final String DEFAULT_PROFILE_SIZE = "h632";

    @PostConstruct
    public void init() {
        log.info("正在初始化 TmdbGetImageService，準備呼叫 TMDB /configuration API...");

        try {
            JsonNode config = tmdbClient.get(new String[]{"configuration"}, Collections.emptyMap());

            if (config != null) {
                String newBaseUrl = config.path("images").path("secure_base_url").asText();
                if (newBaseUrl != null && !newBaseUrl.isBlank()) {
                    this.imageBaseUrl = newBaseUrl;
                    log.info("TMDB 圖片基底 URL 已設定為: {}", this.imageBaseUrl);
                }
            } else {
                log.warn("無法取得 TMDB configuration，將使用預設 URL: {}", this.imageBaseUrl);
            }
        } catch (Exception e) {
            log.error("初始化取得 TMDB configuration 失敗，將使用預設 URL: {}", this.imageBaseUrl, e);
        }
    }

    @Override
    public String getFullImageUrl(String path, String size) {
        if (path == null || path.isBlank() || path.equals("null")) {
            return null;
        }

        String baseUrl = (this.imageBaseUrl != null) ? this.imageBaseUrl : "https://image.tmdb.org/t/p/";

        return baseUrl + size + path;
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