package com.brian.tmov.service;

import com.brian.tmov.dto.request.HistoryRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface HistoryService {

//    新增或更新瀏覽紀錄
    void addHistory(String email, HistoryRequest request);


//    取得瀏覽紀錄列表 (包含 TMDB 詳細資料)
    List<JsonNode> getHistory(String email);

//    刪除單筆紀錄
    void removeHistory(String email, HistoryRequest request);

//    清空所有紀錄
    void clearAllHistory(String email);
}
