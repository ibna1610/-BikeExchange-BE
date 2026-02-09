package com.bikeexchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security Configuration
 * - Cho phép public access tới Swagger UI và tất cả API endpoints
 * - Tương lai: Thêm JWT authentication filter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(authz -> authz
                // Public access to Swagger/OpenAPI documentation
                .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-ui.html")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-ui/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/v3/api-docs")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/v3/api-docs/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-resources")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-resources/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/webjars/**")).permitAll()
                // Temporary: Public access to all APIs (will be secured with JWT)
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).permitAll()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .httpBasic()
            .and()
            .headers().frameOptions().disable();

        return http.build();
    }
}
