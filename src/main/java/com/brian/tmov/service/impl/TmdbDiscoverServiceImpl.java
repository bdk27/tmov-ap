package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.exception.DownstreamException;
import com.brian.tmov.service.TmdbDiscoverService;
import com.brian.tmov.service.TmdbGetImageService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

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

    @Value("${tmdb.default-language:zh-TW}")
    String defaultLanguage;

    private final Random random = new Random();

    @Override
    public Mono<Map<String, String>> getRandomPopularBackdropUrl() {
        Map<String, String> qp = Map.of(
                "language", defaultLanguage,
                "page", "1"
        );

        return tmdbClient.get(new String[]{"movie", "popular"}, qp)
                .map(responseNode -> {
                    JsonNode results = responseNode.path("results");
                    if (results.isArray() && !results.isEmpty()) {
                        JsonNode randomMovie = results.get(random.nextInt(results.size()));
                        String backdropPath = randomMovie.path("backdrop_path").asText(null);

                        String desktopUrl = tmdbGetImageService.getFullImageUrl(backdropPath, "original");
                        String mobileUrl = tmdbGetImageService.getDefaultPosterUrl(backdropPath);

                        Map<String, String> urls = new HashMap<>();
                        urls.put("backdropDesktopUrl", desktopUrl);
                        urls.put("backdropMobileUrl", mobileUrl);

                        return urls;
                    }
                    throw new IllegalArgumentException("無法從 TMDB 取得熱門電影列表");
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    String errorBody = e.getResponseBodyAsString();
                    return Mono.error(new DownstreamException("TMDB API 請求失敗: " + e.getStatusCode() + " " + errorBody, e));
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException || ex instanceof IllegalArgumentException), e -> {
                    return Mono.error(new DownstreamException("TMDB 客戶端發生未知錯誤: " + e.getMessage(), e));
                });
    }
}
