package com.brian.tmov.controller;

import com.brian.tmov.service.TmdbSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tmdb")
public class TmdbController {

    @Autowired
    private TmdbSearchService tmdbSearchService;

    @GetMapping("/search")
    public Mono<ResponseEntity<JsonNode>> search(
            @RequestParam("q") String q,
            @RequestParam(value = "type", defaultValue = "multi") String type,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "includeAdult", required = false) Boolean includeAdult,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "firstAirDateYear", required = false) Integer firstAirDateYear
    ) {
        return tmdbSearchService.search(type, q, page, language, region, includeAdult, year, firstAirDateYear)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class,
                        e -> Mono.just(ResponseEntity.badRequest().body(err(e.getMessage()))))
                .onErrorResume(Exception.class,
                        e -> Mono.just(ResponseEntity.status(502).body(err(e.getMessage()))));
    }

    private com.fasterxml.jackson.databind.node.ObjectNode err(String msg) {
        var n = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        n.put("error", msg); // 中文錯誤訊息由 Service 丟出
        return n;
    }
}
