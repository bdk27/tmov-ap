package com.brian.tmov.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class TmdbClient {

    private final WebClient webClient;

    public TmdbClient(
            WebClient.Builder builder,
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.bearer-token}") String bearerToken
    ) {
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<JsonNode> get(String[] pathSegments, Map<String, String> queryParams) {
        return webClient.get().uri(uriBuilder -> {
            var b = uriBuilder.pathSegment(pathSegments);
            queryParams.forEach((k, v) -> {
                if (v != null && !v.isBlank()) b.queryParam(k, v);
            });
            return b.build();
        }).exchangeToMono(this::handleResponse);
    }

    private Mono<JsonNode> handleResponse(ClientResponse res) {
        if (res.statusCode().is2xxSuccessful()) {
            return res.bodyToMono(JsonNode.class);
        }
        // 將 TMDB 錯誤回傳轉成有意義的訊息
        return res.bodyToMono(JsonNode.class)
                .defaultIfEmpty(null)
                .flatMap(body -> {
                    String msg = "TMDB error: HTTP " + res.statusCode().value();
                    if (body != null && body.has("status_message")) {
                        msg += " - " + body.get("status_message").asText();
                    }
                    return Mono.error(new RuntimeException(msg));
                });
    }
}
