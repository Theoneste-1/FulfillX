package com.fulfillx.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillx.security.JsonAuthenticationEntryPoint;
import com.fulfillx.security.JwtAuthenticationFilter;
import com.fulfillx.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class PaymentSecurityConfig {
    /**
     * Payments are never public. Catalog GET permitAll from the shared resource-server
     * chain does not apply to this service's API.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain paymentApiFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            ObjectMapper objectMapper
    ) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService);
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(objectMapper);
        http
                .securityMatcher("/api/v1/payments/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
