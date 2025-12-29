package com.brian.tmov.service.impl;

import com.brian.tmov.client.TmdbClient;
import com.brian.tmov.dao.entity.HistoryEntity;
import com.brian.tmov.dao.entity.MemberEntity;
import com.brian.tmov.dao.repository.HistoryRepository;
import com.brian.tmov.dao.repository.MemberRepository;
import com.brian.tmov.dto.request.HistoryRequest;
import com.brian.tmov.service.HistoryService;
import com.brian.tmov.service.TmdbGetImageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class HistoryServiceImpl implements HistoryService {

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private TmdbGetImageService tmdbGetImageService;

    @Value("${tmdb.default-language:zh-TW}")
    String defaultLanguage;

    private static final int MAX_HISTORY_SIZE = 20;

    @Override
    @Transactional
    public void addHistory(String email, HistoryRequest request) {
        // 1. 檢查是否已存在
        Optional<HistoryEntity> existing = historyRepository.find(email, request.tmdbId(), request.mediaType());

        if (existing.isPresent()) {
            // 如果已存在，更新時間即可 (Hibernate Dirty Checking 會自動儲存)
            existing.get().setWatchedAt(LocalDateTime.now());
        } else {
            // 如果不存在，建立新的
            MemberEntity member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("會員不存在"));

            HistoryEntity history = new HistoryEntity(member, request.tmdbId(), request.mediaType());
            historyRepository.save(history);

            enforceHistoryLimit(email);
        }
    }

    private void enforceHistoryLimit(String email) {
        List<HistoryEntity> allHistory = historyRepository.findAllByEmail(email);

        if (allHistory.size() > MAX_HISTORY_SIZE) {
            List<HistoryEntity> toDelete = allHistory.subList(MAX_HISTORY_SIZE, allHistory.size());

            historyRepository.deleteAll(toDelete);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<JsonNode> getHistory(String email) {
        List<HistoryEntity> histories = historyRepository.findAllByEmail(email);

        if (histories.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用虛擬執行緒並行抓取 TMDB 詳細資料
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<JsonNode>> futures = new ArrayList<>();

            for (HistoryEntity history : histories) {
                futures.add(executor.submit(() -> fetchDetailsFromTmdb(history)));
            }

            List<JsonNode> resultList = new ArrayList<>();
            for (Future<JsonNode> future : futures) {
                try {
                    JsonNode node = future.get();
                    if (node != null) {
                        resultList.add(node);
                    }
                } catch (ExecutionException e) {
                    log.error("取得歷史紀錄詳情失敗", e);
                }
            }
            return resultList;
        } catch (Exception e) {
            throw new RuntimeException("取得歷史紀錄列表時發生錯誤", e);
        }
    }

    @Override
    @Transactional
    public void removeHistory(String email, HistoryRequest request) {
        historyRepository.remove(email, request.tmdbId(), request.mediaType());
    }

    @Override
    @Transactional
    public void clearAllHistory(String email) {
        historyRepository.removeAllByEmail(email);
    }

    private JsonNode fetchDetailsFromTmdb(HistoryEntity history) {
        String type = history.getMediaType().name();
        String id = String.valueOf(history.getTmdbId());

        try {
            JsonNode json = tmdbClient.get(new String[]{type, id}, Map.of("language", defaultLanguage));

            if (json != null && json.isObject()) {
                ObjectNode obj = (ObjectNode) json;

                // 補上後端資料庫的欄位
                obj.put("history_id", history.getId());
                obj.put("media_type", type);
                obj.put("watched_at", history.getWatchedAt().toString());

                // 處理圖片
                String posterPath = obj.path("poster_path").asText(null);
                String backdropPath = obj.path("backdrop_path").asText(null);
                String profilePath = obj.path("profile_path").asText(null);

                if (posterPath != null) obj.put("full_poster_url", tmdbGetImageService.getDefaultPosterUrl(posterPath));
                if (backdropPath != null) obj.put("full_backdrop_url", tmdbGetImageService.getDefaultBackdropUrl(backdropPath));
                if (profilePath != null) obj.put("full_profile_url", tmdbGetImageService.getDefaultProfileUrl(profilePath));

                return obj;
            }
        } catch (Exception e) {
            log.warn("TMDB 找不到歷史紀錄項目: {}/{}", type, id);
        }
        return null;
    }
}
