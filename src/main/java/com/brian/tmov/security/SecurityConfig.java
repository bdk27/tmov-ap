package com.brian.tmov.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 前後端分離 API 通常關閉 CSRF（不使用表單）
                .csrf(AbstractHttpConfigurer::disable)
                // 先開預設 CORS；下面會再補 CORS 設定 bean
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // 允許測試的公開 API
                        .requestMatchers("/api/tmdb/**").permitAll()
                        // （可選）靜態資源、健康檢查等
                        .requestMatchers("/error", "/actuator/health").permitAll()
                        // 允許所有預檢請求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 其他路徑先要求驗證（之後做登入/收藏會用到）
                        .anyRequest().authenticated()
                )
                // 先用 HTTP Basic 做簡易保護（開發用）
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 關鍵：允許您 Nuxt 前端 (http://localhost:3000) 的來源
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        // 允許所有標準方法
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允許所有標頭
        configuration.setAllowedHeaders(List.of("*"));

        // 關鍵：因為您前端有 credentials: 'include'，所以必須設為 true
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 將此設定套用到所有 API 路徑
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}