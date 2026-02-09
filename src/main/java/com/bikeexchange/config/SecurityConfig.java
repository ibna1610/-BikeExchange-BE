package com.bikeexchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
                // Allow public access to Swagger/OpenAPI docs
                .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-ui.html")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-ui/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/v3/api-docs")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/v3/api-docs/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-resources")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-resources/**")).permitAll()
                .requestMatchers(AntPathRequestMatcher.antMatcher("/webjars/**")).permitAll()
                // Allow public access to all APIs (tạm thời - để test)
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).permitAll()
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            .httpBasic()
            .and()
            .headers().frameOptions().disable();

        return http.build();
    }
}
