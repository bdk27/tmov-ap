package com.brian.tmov.service;

import com.brian.tmov.dto.request.TmdbSearchQueryRequest;
import com.fasterxml.jackson.databind.JsonNode;

public interface TmdbSearchService {

    JsonNode search(TmdbSearchQueryRequest query);
}
