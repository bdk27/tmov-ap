package com.brian.tmov.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface TmdbSearchService {

    /**
     * 代理 TMDB /search/{type}
     * @param type multi|movie|tv|person（預設 multi）
     * @param query 關鍵字（必填）
     * @param page 頁碼（>=1，預設 1）
     * @param language 語系（預設 zh-TW，可前端覆蓋）
     * @param region 區域（預設 TW）
     * @param includeAdult 是否包含成人內容
     * @param year (type=movie 才有用)
     * @param firstAirDateYear (type=tv 才有用)
     */
    Mono<JsonNode> search(
            String type,
            String query,
            Integer page,
            String language,
            String region,
            Boolean includeAdult,
            Integer year,
            Integer firstAirDateYear
    );
}
