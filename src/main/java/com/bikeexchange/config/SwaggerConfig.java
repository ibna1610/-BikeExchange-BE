package com.bikeexchange.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("BikeExchange API")
                                                .version("1.0.0")
                                                .description("Backend API Documentation for BikeExchange Platform - " +
                                                                "Nền tảng mua bán xe đạp thể thao cũ")
                                                .contact(new Contact()
                                                                .name("BikeExchange Team")
                                                                .email("support@bikeexchange.com")
                                                                .url("https://bikeexchange.com"))
                                                .license(new License()
                                                                .name("Apache 2.0")
                                                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                                .components(new io.swagger.v3.oas.models.Components()
                                                .addSecuritySchemes("Bearer Token",
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("JWT Token cho Authentication")));
        }
}
