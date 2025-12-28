package com.core.auth.config;

import com.core.auth.web.ExceptionLoggingFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final ExceptionLoggingFilter exceptionLoggingFilter;
    private final JwtAuthFilter jwtAuthFilter;
    private final FeatureProperties features;   // ⬅️ inject feature flags

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // pakai CorsConfigurationSource dari CorsConfig
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    log.warn("401 {} {} - {}", req.getMethod(), req.getRequestURI(), e.getMessage());
                    res.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter().write("""
                        {"status":"error","data":null,
                         "error":{"code":"unauthorized","message":"Unauthorized","details":null}}
                        """.trim());
                })
                .accessDeniedHandler((req, res, e) -> {
                    log.warn("403 {} {} - {}", req.getMethod(), req.getRequestURI(), e.getMessage());
                    res.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json");
                    res.getWriter().write("""
                        {"status":"error","data":null,
                         "error":{"code":"forbidden","message":"Access is denied","details":null}}
                        """.trim());
                })
            )

            .authorizeHttpRequests(auth -> {
                // ⬇⬇⬇ BARIS PENTING: IZINKAN SEMUA PRE-FLIGHT OPTIONS
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                // ===== PUBLIC AUTH =====
                auth.requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/auth/login/totp",
                    "/auth/refresh",
                    "/auth/logout",
                    "/auth/verification/email/request",
                    "/auth/verification/email/confirm",
                    "/auth/password/request",
                    "/auth/password/confirm"
                ).permitAll();

                // ===== OPENAPI / SWAGGER (pakai flag) =====
                if (features.isOpenapiEnabled()) {
                    auth.requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                    ).permitAll();
                } else {
                    auth.requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                    ).denyAll();
                }

                // ===== ACTUATOR (pakai flag) =====
                if (features.isActuatorEnabled()) {
                    auth.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    auth.requestMatchers("/actuator/**").hasAuthority("role.manage");
                } else {
                    auth.requestMatchers("/actuator/**").denyAll();
                }

                // ===== DEBUG TOTP DEV ONLY =====
                auth.requestMatchers("/mfa/totp/_debug-code/**").permitAll();

                // ===== BUSINESS API =====
                auth.requestMatchers("/api-keys/**").authenticated();
                auth.requestMatchers("/admin/**").hasAuthority("role.manage");
                auth.requestMatchers("/merchant/**").authenticated();
                auth.requestMatchers("/me/**").authenticated();
                auth.requestMatchers("/mfa/totp/**").authenticated();

                // WebAuthn nanti:
                // if (features.isWebauthnEnabled()) {
                //   auth.requestMatchers("/webauthn/**").permitAll() atau pola lain
                // } else {
                //   auth.requestMatchers("/webauthn/**").denyAll();
                // }

                auth.anyRequest().authenticated();
            })

            .addFilterBefore(exceptionLoggingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
