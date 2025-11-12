package com.brian.tmov.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface TmdbResponseTransformerService {

    JsonNode transformSearchResponse(JsonNode responseNode);
}
