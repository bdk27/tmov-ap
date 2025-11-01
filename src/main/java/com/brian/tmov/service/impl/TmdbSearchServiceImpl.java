package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.dto.TmdbSearchQuery;
import com.brian.tmov.enums.TmdbSearchType;
import com.brian.tmov.exception.DownstreamException;
import com.brian.tmov.service.TmdbSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TmdbSearchServiceImpl implements TmdbSearchService {

    @Autowired
    private TmdbClient tmdbClient;

    @Value("${tmdb.default-language:zh-TW}")
    String defaultLanguage;

    @Value("${tmdb.default-region:TW}")
    String defaultRegion;

    @Override
    public Mono<JsonNode> search(TmdbSearchQuery query) {
        final TmdbSearchType t = TmdbSearchType.from(query.typeOrDefault());

        Map<String, String> qp = getStringMap(query, t);

        return tmdbClient.get(new String[]{"search", t.value()}, qp)
                .onErrorResume(WebClientResponseException.class, e -> {
                    String errorBody = e.getResponseBodyAsString();
                    return Mono.error(new DownstreamException("TMDB API 請求失敗: " + e.getStatusCode() + " " + errorBody, e));
                })
                .onErrorResume(ex -> !(ex instanceof DownstreamException || ex instanceof IllegalArgumentException), e -> {
                    // 捕獲其他 tmdbClient 可能的錯誤 (例如連線逾時)
                    return Mono.error(new DownstreamException("TMDB 客戶端發生未知錯誤: " + e.getMessage(), e));
                });
    }

    private Map<String, String> getStringMap(TmdbSearchQuery query, TmdbSearchType t) {
        Map<String, String> qp = new LinkedHashMap<>();
        qp.put("query", query.q());
        qp.put("page", String.valueOf(query.pageOrDefault()));
        qp.put("language", query.languageOrDefault(defaultLanguage));
        qp.put("region", query.regionOrDefault(defaultRegion));

        if (query.includeAdult() != null) {
            qp.put("include_adult", query.includeAdult().toString());
        }
        if (t == TmdbSearchType.MOVIE && query.year() != null) {
            qp.put("year", String.valueOf(query.year()));
        }
        if (t == TmdbSearchType.TV && query.firstAirDateYear() != null) {
            qp.put("first_air_date_year", String.valueOf(query.firstAirDateYear()));
        }
        return qp;
    }
}
