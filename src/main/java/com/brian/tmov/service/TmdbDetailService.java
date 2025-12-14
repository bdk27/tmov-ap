package com.brian.tmov.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface TmdbDetailService {

//    電影詳情
    Mono<JsonNode> getMovieDetail(long movieId);

//    電視節目詳情
    Mono<JsonNode> getTvDetail(long tvId);

//    人物詳情
    Mono<JsonNode> getPersonDetail(long personId);
}
