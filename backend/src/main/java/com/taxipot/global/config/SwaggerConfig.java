package com.taxipot.global.config;

import com.taxipot.global.auth.CurrentUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** http://localhost:8080/swagger-ui.html — 오른쪽 위 Authorize에 accessToken 입력 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("택시팟 API").version("3.9.0"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /** JWT 완성 전 개발용: 모든 API에 X-Debug-User-Id 입력칸 추가 (app.auth.debug-header-enabled=true일 때만) */
    @Bean
    public OperationCustomizer debugUserHeader(AppProperties appProperties) {
        return (operation, handlerMethod) -> {
            if (appProperties.auth() != null && appProperties.auth().debugHeaderEnabled()) {
                operation.addParametersItem(new HeaderParameter().name(CurrentUser.DEBUG_HEADER)
                        .description("개발용 로그인 회원 id (local 전용)").required(false).schema(new StringSchema()));
            }
            return operation;
        };
    }
}
