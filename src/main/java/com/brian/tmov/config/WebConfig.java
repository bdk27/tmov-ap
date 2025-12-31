package com.brian.tmov.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 取得 uploads 資料夾的絕對路徑
        String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();

        // 設定路徑對應
        // 網址輸入: http://localhost:8080/images/xxx.jpg
        // 實際讀取: 專案根目錄/uploads/xxx.jpg
        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadPath);
    }
}
