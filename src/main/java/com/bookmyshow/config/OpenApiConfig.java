package com.bookmyshow.config;

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

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "ClerkAuthHeader";
        final String adminRoleHeader = "AdminRoleHeader";
        final String adminEmailHeader = "AdminEmailHeader";

        return new OpenAPI()
                .info(new Info()
                        .title("CineX Movie Ticket Booking Platform API")
                        .version("1.0.0-PROD")
                        .description("Production-grade movie ticket booking platform API featuring TMDB integration, digital QR ticketing, Razorpay payment lifecycle, Redis caching, and dynamic seating layouts.")
                        .contact(new Contact()
                                .name("CineX Technical Lead")
                                .email("admin@cinex.com")
                                .url("https://cinex-platform.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name("Authorization")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Clerk Bearer / Authorization Header for User Authentication"))
                        .addSecuritySchemes(adminRoleHeader,
                                new SecurityScheme()
                                        .name("X-User-Role")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Admin Role Verification Header (e.g., ADMIN)"))
                        .addSecuritySchemes(adminEmailHeader,
                                new SecurityScheme()
                                        .name("X-User-Email")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Admin Email Verification Header")));
    }
}
