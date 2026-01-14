package com.brian.tmov.controller;

import com.brian.tmov.dto.request.FavoriteRequest;
import com.brian.tmov.service.FavoriteService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Tag(name = "收藏功能", description = "加入/移除收藏與查詢收藏列表")
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Operation(summary = "取得我的收藏", description = "取得使用者的所有收藏項目")
    @GetMapping
    public ResponseEntity<List<JsonNode>> getMyFavorites(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        List<JsonNode> favorites = favoriteService.getFavorites(principal.getName());
        return ResponseEntity.ok(favorites);
    }

    @Operation(summary = "加入收藏", description = "將電影、影集或人物加入收藏清單")
    @PostMapping
    public ResponseEntity<?> addFavorite(
            @Valid @RequestBody FavoriteRequest request,
            Principal principal // 自動取得當前登入使用者的資訊
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        favoriteService.addFavorite(principal.getName(), request);
        return ResponseEntity.ok(Map.of("message", "已加入收藏"));
    }

    @Operation(summary = "移除收藏", description = "從收藏清單中移除")
    @DeleteMapping
    public ResponseEntity<?> removeFavorite(
            @Valid @RequestBody FavoriteRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        favoriteService.removeFavorite(principal.getName(), request);
        return ResponseEntity.ok(Map.of("message", "已移除收藏"));
    }

    @Operation(summary = "檢查收藏狀態", description = "確認某項目是否已被當前使用者收藏")
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
