package com.taxipot.global.config;

import com.taxipot.global.security.JwtAuthenticationFilter;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI (http://localhost:8080/swagger-ui.html)
 * <ul>
 *   <li>'택시팟 API 명세서 3.9.0 (공식)': 팀이 합의한 원본 yaml — 기준 문서</li>
 *   <li>'서버 구현 (자동)': 실제 서버 코드에서 만든 문서 — 테스트 호출용</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(LoginUserId.class);
    }

    @Bean
    public OpenAPI taxipotOpenApi() {
        return new OpenAPI()
                .info(new Info().title("택시팟 서버 (구현)").version("3.9.0"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /** local 프로필: 모든 API 에 X-Debug-User-Id 입력 칸 추가 (JWT 없이 회원 지정) */
    @Bean
    public OperationCustomizer debugUserHeaderCustomizer(AppProperties props) {
        return (operation, handlerMethod) -> {
            if (props.security().debugHeaderEnabled()) {
                operation.addParametersItem(new HeaderParameter()
                        .name(JwtAuthenticationFilter.DEBUG_HEADER)
                        .description("local 전용: 로그인 없이 이 회원 id 로 호출 (Authorization 이 있으면 무시)")
                        .required(false)
                        .schema(new StringSchema()));
            }
            return operation;
        };
    }
}
