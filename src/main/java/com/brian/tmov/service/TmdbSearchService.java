package com.brian.tmov.service;

import com.brian.tmov.dto.TmdbSearchQuery;
import com.fasterxml.jackson.databind.JsonNode;

public interface TmdbSearchService {

    JsonNode search(TmdbSearchQuery query);
}
