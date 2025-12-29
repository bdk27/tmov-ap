package com.brian.tmov.controller;

import com.brian.tmov.dto.request.HistoryRequest;
import com.brian.tmov.service.HistoryService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private HistoryService historyService;

//    新增/更新 觀看紀錄
    @PostMapping
    public ResponseEntity<?> addHistory(
            @Valid @RequestBody HistoryRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        historyService.addHistory(principal.getName(), request);
        return ResponseEntity.ok(Map.of("message", "已更新觀看紀錄"));
    }

//    取得我的觀看紀錄
    @GetMapping
    public ResponseEntity<List<JsonNode>> getMyHistory(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        List<JsonNode> history = historyService.getHistory(principal.getName());
        return ResponseEntity.ok(history);
    }

//    刪除單筆紀錄
    @DeleteMapping
    public ResponseEntity<?> removeHistory(
            @Valid @RequestBody HistoryRequest request,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        historyService.removeHistory(principal.getName(), request);
        return ResponseEntity.ok(Map.of("message", "已刪除紀錄"));
    }

//    清空所有紀錄
    @DeleteMapping("/all")
    public ResponseEntity<?> clearAllHistory(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        historyService.clearAllHistory(principal.getName());
        return ResponseEntity.ok(Map.of("message", "已清空所有紀錄"));
    }}
