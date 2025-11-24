package com.core.auth.config;

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

import com.core.auth.web.ExceptionLoggingFilter;


@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final ExceptionLoggingFilter exceptionLoggingFilter; 

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ⬇️ Tambahkan blok ini
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    log.warn("401 {} {} - {}", req.getMethod(), req.getRequestURI(), e.getMessage());
                    res.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":\"error\",\"data\":null," +
                            "\"error\":{\"code\":\"unauthorized\",\"message\":\"Unauthorized\",\"details\":null}}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    log.warn("403 {} {} - {}", req.getMethod(), req.getRequestURI(), e.getMessage());
                    res.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":\"error\",\"data\":null," +
                            "\"error\":{\"code\":\"forbidden\",\"message\":\"Access is denied\",\"details\":null}}");
                })
            )
            // ⬆️ Tambahkan sampai sini

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/register", "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .requestMatchers(
                        "/auth/verification/email/request",
                        "/auth/verification/email/confirm",
                        "/auth/password/request",
                        "/auth/password/confirm"
                ).permitAll()
                .requestMatchers("/api-keys/**").authenticated() 
                .requestMatchers("/admin/**").hasAuthority("role.manage")
                .requestMatchers("/merchant/**").authenticated()
                .requestMatchers("/me/**").authenticated()
                .anyRequest().authenticated())

            // ⬇️ pasang logger filter lebih dulu
            .addFilterBefore(exceptionLoggingFilter, UsernamePasswordAuthenticationFilter.class)
    // lalu JWT filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
