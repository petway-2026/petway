package com.petway.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.PathItem;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.stream.Stream;

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

    @Bean
    public OpenApiCustomizer csrfHeaderCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                csrfProtectedOperations(pathItem).forEach(operation ->
                        operation.addParametersItem(new HeaderParameter()
                                .name("X-CSRF-TOKEN")
                                .description("먼저 GET /api/auth/csrf를 호출해 받은 token 값")
                                .required(true)
                                .schema(new StringSchema()))));
    }

    private Stream<Operation> csrfProtectedOperations(
            PathItem pathItem
    ) {
        return Stream.of(
                pathItem.getPost(),
                pathItem.getPut(),
                pathItem.getPatch(),
                pathItem.getDelete()
        ).filter(Objects::nonNull);
    }
}
