package com.brian.tmov.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                // 2. CSRF 在 API 模式通常關閉
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // 3. CORS 設定
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 4. 權限設定 (注意：這裡用 authorizeExchange 而不是 authorizeHttpRequests)
                .authorizeExchange(exchanges -> exchanges
                        // 允許 OPTIONS 預檢請求
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        // 允許所有 TMDB API
                        .pathMatchers("/api/tmdb/**").permitAll()
                        // 允許健康檢查等端點
                        .pathMatchers("/actuator/**", "/error").permitAll()
                        // 其他所有請求都需要驗證
                        .anyExchange().authenticated()
                )
                // 5. 使用 HTTP Basic 驗證 (開發用)
                .httpBasic(withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允許的前端來源 (Nuxt)
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        // 允許的方法
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允許的標頭
        configuration.setAllowedHeaders(List.of("*"));

        // 允許攜帶憑證 (Cookies)
        configuration.setAllowCredentials(true);

        // 注意：這裡是 reactive package 下的 UrlBasedCorsConfigurationSource
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}