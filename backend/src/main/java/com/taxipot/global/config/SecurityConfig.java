package com.taxipot.global.config;

import com.taxipot.global.error.SecurityErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 1단계(지금): 모든 API 허용. 개발 중에는 Swagger의 X-Debug-User-Id 칸으로 로그인 사용자를 흉내 낸다.
 * 2단계(기성, JWT 완성 후): 아래 TODO 두 곳만 바꾼다. 다른 팀원 코드는 그대로 (@LoginUserId가 그대로 동작).
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** 명세에서 security: []인 API + 개발 도구. JWT 적용 후에도 토큰 없이 호출 가능 */
    public static final String[] PUBLIC_POST = {"/api/auth/signup", "/api/auth/login", "/api/auth/refresh"};
    public static final String[] PUBLIC_GET = {"/api/auth/nickname-check", "/api/universities", "/api/banks",
            "/api/health"};
    public static final String[] PUBLIC_ANY = {"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
            "/error", "/ws/**"}; // /ws: STOMP는 CONNECT 프레임에서 토큰 검사

    private final SecurityErrorHandler securityErrorHandler;
    private final AppProperties appProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers(PUBLIC_ANY).permitAll()
                        // TODO(기성, JWT 후): 아래 한 줄을 두 줄로 교체
                        //   .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        //   .anyRequest().authenticated()
                        .anyRequest().permitAll());
        // TODO(기성, JWT 후): http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        //   필터는 인증 성공 시 principal = 회원 id(Long), 권한 = ROLE_USER / ROLE_ADMIN 으로 SecurityContext에 넣는다.
        //   토큰 버전 불일치는 securityErrorHandler.write(response, ErrorCode.TOKEN_REVOKED)
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 운영자 웹을 다른 주소(예: localhost:5173)에서 띄울 때만 필요. 앱(Android)은 CORS와 무관 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = appProperties.cors() == null ? null : appProperties.cors().allowedOrigins();
        config.setAllowedOrigins(origins == null ? List.of() : origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
