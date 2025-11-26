package com.core.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TotpProperties.class)
public class AppConfig {
    // kosong juga tidak apa-apa, tugasnya cuma mengaktifkan TotpProperties
}
