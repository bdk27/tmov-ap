package com.brian.tmov.service;

import com.brian.tmov.dto.request.FavoriteRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface FavoriteService {

//    取得收藏
    List<JsonNode> getFavorites(String email);

//    加入收藏
    void addFavorite(String email, FavoriteRequest request);

//    移除收藏
    void removeFavorite(String email, FavoriteRequest request);

//    檢查是否已收藏 (回傳 true/false)
    boolean isFavorite(String email, Long tmdbId, String mediaTypeStr);
}
