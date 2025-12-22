package com.brian.tmov.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface TmdbDetailService {

//    電影詳情
    JsonNode getMovieDetail(long movieId);

//    電視節目詳情
    JsonNode getTvDetail(long tvId);

//    人物詳情
    JsonNode getPersonDetail(long personId);
}
