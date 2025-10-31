package com.brian.tmov.service;

import com.brian.tmov.dto.TmdbSearchQuery;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface TmdbSearchService {

    Mono<JsonNode> search(TmdbSearchQuery query);
}
