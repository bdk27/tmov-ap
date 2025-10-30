package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.enums.TmdbSearchType;
import com.brian.tmov.service.TmdbSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
    public Mono<JsonNode> search(String type, String query, Integer page, String language, String region, Boolean includeAdult, Integer year, Integer firstAirDateYear) {
        String normalized = (type == null || type.isBlank()) ? "multi" : type.toLowerCase();

        final TmdbSearchType t = TmdbSearchType.from(type);
        if (query == null || query.isBlank()) {
            return Mono.error(new IllegalArgumentException("請輸入關鍵字"));
        }

        Map<String, String> qp = new LinkedHashMap<>();
        qp.put("query", query);
        qp.put("page", String.valueOf(page == null ? 1 : Math.max(1, page)));
        qp.put("language", (language == null || language.isBlank()) ? defaultLanguage : language);
        qp.put("region", (region == null || region.isBlank()) ? defaultRegion : region);
        if (includeAdult != null) qp.put("include_adult", includeAdult.toString());
        if (t == TmdbSearchType.MOVIE && year != null) qp.put("year", String.valueOf(year));
        if (t == TmdbSearchType.TV && firstAirDateYear != null) qp.put("first_air_date_year", String.valueOf(firstAirDateYear));

        return tmdbClient.get(new String[]{"search", normalized}, qp);
    }
}
