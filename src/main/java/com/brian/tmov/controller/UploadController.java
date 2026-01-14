package com.brian.tmov.controller;

import com.brian.tmov.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "檔案上傳", description = "處理圖片上傳功能")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private FileService fileService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Operation(summary = "上傳圖片", description = "上傳圖片檔案並回傳可存取的 URL")
    @PostMapping
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        // 儲存檔案
        String fileName = fileService.storeFile(file);

        // 產生可存取的 URL
        // 結果會像: http://localhost:8080/images/檔名.jpg
        String fileDownloadUri = appBaseUrl + "/images/" + fileName;

        return ResponseEntity.ok(Map.of("url", fileDownloadUri));
    }
}
