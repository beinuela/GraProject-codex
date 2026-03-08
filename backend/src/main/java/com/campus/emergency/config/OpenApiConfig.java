package com.campus.emergency.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI emergencyOpenApi() {
        return new OpenAPI()
                .info(new Info().title("校园应急物资智能管理系统 API").version("1.0.0").description("Backend API"));
    }
}
