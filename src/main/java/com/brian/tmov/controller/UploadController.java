package com.brian.tmov.controller;

import com.brian.tmov.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private FileService fileService;

    @Value("${app.base-url}")
    private String appBaseUrl;

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
