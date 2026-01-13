package com.brian.tmov.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI tmovOpenAPI() {
        // 定義安全驗證名稱
        String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                // 設定文件基本資訊
                .info(new Info()
                        .title("TMOV 影音平台 API 文件")
                        .description("提供電影查詢、訂票、會員管理等功能")
                        .version("1.0.0"))

                // 設定全域安全需求 (所有 API 預設都需要此驗證，除非在 Controller 覆寫)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                // 定義安全驗證組件 (JWT)
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
