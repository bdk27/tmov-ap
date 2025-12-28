package com.brian.tmov.controller;

import com.brian.tmov.dto.request.FavoriteRequest;
import com.brian.tmov.service.FavoriteService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

//    取得收藏
    @GetMapping
    public ResponseEntity<List<JsonNode>> getMyFavorites(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        List<JsonNode> favorites = favoriteService.getFavorites(principal.getName());
        return ResponseEntity.ok(favorites);
    }

//    加入收藏
    @PostMapping
    public ResponseEntity<?> addFavorite(
            @Valid @RequestBody FavoriteRequest request,
            Principal principal // 自動取得當前登入使用者的資訊
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        favoriteService.addFavorite(principal.getName(), request);
        return ResponseEntity.ok(Map.of("message", "已加入收藏"));
    }

//    取消收藏
    @DeleteMapping
    public ResponseEntity<?> removeFavorite(
            @Valid @RequestBody FavoriteRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        favoriteService.removeFavorite(principal.getName(), request);
        return ResponseEntity.ok(Map.of("message", "已移除收藏"));
    }

//    檢查是否已收藏
    @GetMapping("/check")
    public ResponseEntity<?> checkStatus(
            @RequestParam Long tmdbId,
            @RequestParam String mediaType,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.ok(Map.of("isFavorite", false));

        boolean isFav = favoriteService.isFavorite(principal.getName(), tmdbId, mediaType);
        return ResponseEntity.ok(Map.of("isFavorite", isFav));
    }
}
