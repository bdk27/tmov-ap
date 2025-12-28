package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.dao.entity.FavoriteEntity;
import com.brian.tmov.dao.entity.MemberEntity;
import com.brian.tmov.dao.repository.FavoriteRepository;
import com.brian.tmov.dao.repository.MemberRepository;
import com.brian.tmov.dto.request.FavoriteRequest;
import com.brian.tmov.enums.MediaType;
import com.brian.tmov.service.FavoriteService;
import com.brian.tmov.service.TmdbGetImageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private TmdbGetImageService tmdbGetImageService;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Value("${tmdb.default-language:zh-TW}")
    String defaultLanguage;

    @Override
    @Transactional(readOnly = true)
    public List<JsonNode> getFavorites(String email) {
        // 從資料庫撈出所有收藏 ID
        List<FavoriteEntity> favorites = favoriteRepository.findAllByEmail(email);

        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用虛擬執行緒並行發送請求給 TMDB
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<JsonNode>> futures = new ArrayList<>();

            for (FavoriteEntity fav : favorites) {
                futures.add(executor.submit(() -> fetchDetailsFromTmdb(fav)));
            }

            // 收集結果
            List<JsonNode> resultList = new ArrayList<>();
            for (Future<JsonNode> future : futures) {
                try {
                    JsonNode node = future.get(); // 這裡會等待任務完成
                    if (node != null) {
                        resultList.add(node);
                    }
                } catch (ExecutionException e) {
                    log.error("取得收藏詳情失敗", e);
                }
            }
            return resultList;
        } catch (Exception e) {
            throw new RuntimeException("取得收藏列表時發生錯誤", e);
        }
    }

    @Override
    @Transactional
    public void addFavorite(String email, FavoriteRequest request) {
        if (favoriteRepository.check(
                email, request.tmdbId(), request.mediaType())) {
            throw new IllegalArgumentException("已在收藏清單中");
        }

        MemberEntity member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("會員不存在"));

        FavoriteEntity favorite = new FavoriteEntity(member, request.tmdbId(), request.mediaType());
        favoriteRepository.save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(String email, FavoriteRequest request) {
        favoriteRepository.remove(
                email, request.tmdbId(), request.mediaType()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(String email, Long tmdbId, String mediaTypeStr) {
        try {
            MediaType type = MediaType.valueOf(mediaTypeStr.toLowerCase());
            return favoriteRepository.check(email, tmdbId, type);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private JsonNode fetchDetailsFromTmdb(FavoriteEntity fav) {
        String type = fav.getMediaType().name(); // "movie", "tv", "person"
        String id = String.valueOf(fav.getTmdbId());

        try {
            // 呼叫 TMDB 取得基本資料
            JsonNode json = tmdbClient.get(new String[]{type, id}, Map.of("language", defaultLanguage));

            if (json != null && json.isObject()) {
                ObjectNode obj = (ObjectNode) json;

                // 補上資料庫的 favorite_id (方便前端做刪除操作)
                obj.put("favorite_id", fav.getId());
                // 補上 media_type (因為 TMDB 詳情 API 預設不回傳這個，但前端列表需要)
                obj.put("media_type", type);

                // 處理圖片網址
                String posterPath = obj.path("poster_path").asText(null);
                String backdropPath = obj.path("backdrop_path").asText(null);
                String profilePath = obj.path("profile_path").asText(null);

                if (posterPath != null) obj.put("full_poster_url", tmdbGetImageService.getDefaultPosterUrl(posterPath));
                if (backdropPath != null) obj.put("full_backdrop_url", tmdbGetImageService.getDefaultBackdropUrl(backdropPath));
                if (profilePath != null) obj.put("full_profile_url", tmdbGetImageService.getDefaultProfileUrl(profilePath));

                return obj;
            }
        } catch (Exception e) {
            // 如果 TMDB 找不到這部片 (可能已被刪除)，我們就回傳 null 把它過濾掉
            log.warn("TMDB 找不到收藏項目: {}/{}", type, id);
        }
        return null;
    }
}
