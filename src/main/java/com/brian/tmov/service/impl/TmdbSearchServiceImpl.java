package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.dto.request.TmdbSearchQueryRequest;
import com.brian.tmov.enums.TmdbSearchType;
import com.brian.tmov.service.TmdbResponseTransformerService;
import com.brian.tmov.service.TmdbSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class TmdbSearchServiceImpl implements TmdbSearchService {

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private TmdbResponseTransformerService tmdbResponseTransformerService;

    @Value("${tmdb.default-language:zh-TW}")
    String defaultLanguage;

    @Value("${tmdb.default-region:TW}")
    String defaultRegion;

    @Override
    public JsonNode search(TmdbSearchQueryRequest query) {
        final TmdbSearchType t = TmdbSearchType.from(query.typeOrDefault());

        Map<String, String> qp = getStringMap(query, t);

        JsonNode result = tmdbClient.get(new String[]{"search", t.value()}, qp);

        // 加工圖片網址
        return tmdbResponseTransformerService.transformSearchResponse(result);
    }

    private Map<String, String> getStringMap(TmdbSearchQueryRequest query, TmdbSearchType t) {
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
