package com.brian.tmov.client;

import com.brian.tmov.exception.DownstreamException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class TmdbClient {

    private final RestClient restClient;

    public TmdbClient(
            @Value("${tmdb.bearer-token}") String bearerToken,
            @Value("${tmdb.base-url}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + bearerToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public JsonNode get(String[] paths, Map<String, String> queryParams) {
        String path = String.join("/", paths);

        // 組合 URL
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath(path);
        if (queryParams != null) {
            queryParams.forEach(uriBuilder::queryParam);
        }

        try {
            return restClient.get()
                    .uri(uriBuilder.build().toUriString())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new DownstreamException("呼叫 TMDB API 失敗: " + path, e);
        }
    }
}
