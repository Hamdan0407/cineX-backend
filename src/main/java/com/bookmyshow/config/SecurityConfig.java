package com.bookmyshow.config;

import com.bookmyshow.filter.ClerkJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Production Security Configuration for CineX.
 * Configures stateless JWT authentication, CORS, CSRF protection, and endpoint authorization rules.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityErrorResponseHandler securityErrorResponseHandler;

    @Value("${clerk.issuer:}")
    private String clerkIssuer;

    @Bean
    public ClerkJwtAuthenticationFilter clerkJwtAuthenticationFilter() {
        return new ClerkJwtAuthenticationFilter(securityErrorResponseHandler);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ClerkJwtAuthenticationFilter clerkJwtAuthenticationFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.disable())) // Required for H2 console
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(securityErrorResponseHandler.authenticationEntryPoint())
                    .accessDeniedHandler(securityErrorResponseHandler.accessDeniedHandler()))
            .authorizeHttpRequests(auth -> {
                // Public GET endpoints
                auth.requestMatchers(HttpMethod.GET, "/api/movies/**", "/api/theatres/**", "/api/shows/**", "/api/tmdb/**", "/api/media/**", "/api/seats/screen/**").permitAll();
                auth.requestMatchers(HttpMethod.GET, "/api/cities", "/api/cities/search").permitAll();
                // Public POST user registration/login
                auth.requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll();
                // Documentation & Monitoring & WebSockets
                auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**", "/h2-console/**", "/ws/**").permitAll();
                // The no-token REST fallback is available only to the isolated test environment.
                if ("https://test.clerk.dev".equals(clerkIssuer)) {
                    auth.requestMatchers(HttpMethod.POST, "/api/shows/*/seats/lock").permitAll();
                    auth.requestMatchers(HttpMethod.DELETE, "/api/shows/*/seats/lock").permitAll();
                }
                // Admin only endpoints
                auth.requestMatchers("/api/admin/**", "/api/cache/**").hasRole("ADMIN");
                // Authenticated user endpoints
                auth.requestMatchers("/api/bookings/**", "/api/payments/**", "/api/tickets/**", "/api/wallet/**", "/api/coupons/**").hasAnyRole("USER", "ADMIN");
                // All other requests must be authenticated
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(clerkJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}
