package com.brian.tmov.service;

import com.brian.tmov.dto.request.FavoriteRequest;

public interface FavoriteService {

//    加入收藏
    void addFavorite(String email, FavoriteRequest request);

//    移除收藏
    void removeFavorite(String email, FavoriteRequest request);

//    檢查是否已收藏 (回傳 true/false)
    boolean isFavorite(String email, Long tmdbId, String mediaTypeStr);
}
