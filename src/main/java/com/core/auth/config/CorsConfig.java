package com.core.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // PAKAI PATTERN biar fleksibel (boleh localhost, 127.0.0.1, bahkan IP LAN)
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.*:*",
                "http://10.*:*"
        ));
        // Bisa juga sementara pakai: List.of("*") untuk full wildcard dev-only

        // IJINKAN SEMUA METHOD
        config.setAllowedMethods(List.of("*"));

        // IJINKAN SEMUA HEADER
        config.setAllowedHeaders(List.of("*"));

        // UNTUK SEKARANG: nggak usah pakai credentials (cookie) dulu
        // supaya nggak ada drama kombinasi origin + credentials
        config.setAllowCredentials(false);

        // Cache preflight 1 jam
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
