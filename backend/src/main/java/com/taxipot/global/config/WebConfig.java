package com.taxipot.global.config;

import com.taxipot.global.auth.LoginUserIdArgumentResolver;
import com.taxipot.global.auth.UnpaidDebtInterceptor;
import com.taxipot.global.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginUserIdArgumentResolver loginUserIdArgumentResolver;
    private final UnpaidDebtInterceptor unpaidDebtInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserIdArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(unpaidDebtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/admin/**", "/api/health");
    }

    /**
     * 쿼리 파라미터 일시(GET /api/pots?from=...&to=...) 변환.
     * 앱(Retrofit)이 '+09:00'의 '+'를 인코딩하지 않고 보내면 서버에서는 공백(' ')으로 읽힌다.
     * → 공백을 '+'로 되돌린 뒤 파싱하고 KST로 맞춘다. 오프셋이 없으면 예외 → INVALID_DATETIME_FORMAT.
     * 컨트롤러에서는 @RequestParam(required = false) OffsetDateTime from 만 쓰면 된다 (@DateTimeFormat 불필요).
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, OffsetDateTime>() {
            @Override
            public OffsetDateTime convert(String source) {
                String value = source.trim().replace(' ', '+');
                return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        .atZoneSameInstant(TimeUtil.KST).toOffsetDateTime();
            }
        });
    }
}
