package com.taxipot.global.config;

import com.taxipot.global.security.LoginUserIdArgumentResolver;
import com.taxipot.global.time.OffsetDateTimeParamConverter;
import com.taxipot.global.web.DebtRestrictionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final DebtRestrictionInterceptor debtRestrictionInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginUserIdArgumentResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(debtRestrictionInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new OffsetDateTimeParamConverter());
    }
}
