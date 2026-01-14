package com.brian.tmov.controller;

import com.brian.tmov.dto.request.HistoryRequest;
import com.brian.tmov.service.HistoryService;
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

@Tag(name = "觀看紀錄", description = "管理使用者的最近觀看紀錄")
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private HistoryService historyService;

    @Operation(summary = "更新觀看紀錄", description = "當使用者進入詳情頁時呼叫，會更新該項目的觀看時間")
    @PostMapping
    public ResponseEntity<?> addHistory(
            @Valid @RequestBody HistoryRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        historyService.addHistory(principal.getName(), request);
        return ResponseEntity.ok(Map.of("message", "已更新觀看紀錄"));
    }

    @Operation(summary = "取得我的紀錄", description = "取得使用者最近的瀏覽紀錄 (最多 20 筆)")
    @GetMapping
    public ResponseEntity<List<JsonNode>> getMyHistory(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        List<JsonNode> history = historyService.getHistory(principal.getName());
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "刪除單筆紀錄", description = "從紀錄中移除特定項目")
    @DeleteMapping
    public ResponseEntity<?> removeHistory(
            @Valid @RequestBody HistoryRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        historyService.removeHistory(principal.getName(), request);
        return ResponseEntity.ok(Map.of("message", "已刪除紀錄"));
    }

    @Operation(summary = "清空所有紀錄", description = "刪除該使用者的所有瀏覽紀錄")
    @DeleteMapping("/all")
    public ResponseEntity<?> clearAllHistory(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        historyService.clearAllHistory(principal.getName());
        return ResponseEntity.ok(Map.of("message", "已清空所有紀錄"));
    }}
