package com.brian.tmov.service.impl;

import com.brian.tmov.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    private final Path fileStorageLocation;

    public FileServiceImpl() {
        // 在專案執行目錄下建立 uploads 資料夾
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("無法建立上傳目錄", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // 取得原始檔名
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new IllegalArgumentException("檔名無效");
        }

        // 產生唯一的檔名
        // 例如: uuid.jpg
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String newFileName = UUID.randomUUID() + fileExtension;

        try {
            // 複製檔案到目標路徑
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 回傳新檔名 (Controller 會把它組合成 URL)
            return newFileName;
        } catch (IOException ex) {
            throw new RuntimeException("儲存檔案失敗 " + newFileName, ex);
        }
    }
}
