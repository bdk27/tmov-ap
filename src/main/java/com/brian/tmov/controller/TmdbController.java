package com.brian.tmov.controller;

import com.brian.tmov.dto.TmdbSearchQuery;
import com.brian.tmov.service.TmdbDiscoverService;
import com.brian.tmov.service.TmdbSearchService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tmdb")
public class TmdbController {

    @Autowired
    private TmdbSearchService tmdbSearchService;

    @Autowired
    private TmdbDiscoverService tmdbDiscoverService;

    @GetMapping("/search")
    public Mono<ResponseEntity<JsonNode>> search(@Valid TmdbSearchQuery query) {
        return tmdbSearchService.search(query).map(ResponseEntity::ok);
    }

    @GetMapping("/popular-backdrop")
    public Mono<ResponseEntity<JsonNode>> getRandomPopularBackdrop() {
        return tmdbDiscoverService.getRandomPopularBackdropUrl()
                .map(url -> {
                    ObjectNode json = JsonNodeFactory.instance.objectNode();
                    json.put("backdrop_url", url);
                    return ResponseEntity.ok().body((JsonNode) json);
                });
    }
}
