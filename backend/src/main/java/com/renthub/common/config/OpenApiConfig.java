package com.renthub.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI rentHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RentHub - Peer-to-Peer Equipment Rental Marketplace API")
                        .description("Production-grade RESTful API for RentHub marketplace. Built with Spring Boot 3, Java 17/21, Spring Security, JWT, and Hibernate.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("RentHub Engineering Team")
                                .email("engineering@renthub.com")
                                .url("https://renthub.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Provide a valid JWT token")));
    }
}
