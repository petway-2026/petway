package com.petway.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI petWayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PetWay Backend API")
                        .version("v1")
                        .description(
                                "PetWay 반려동물 동반 관광 서비스 Backend API"
                        ));
    }
}
