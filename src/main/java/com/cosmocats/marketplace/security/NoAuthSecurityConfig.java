package com.cosmocats.marketplace.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Local development without authentication: {@code ./gradlew bootRun --args='--spring.profiles.active=no-auth'}.
 * Every request is allowed and method security is not enabled. Never use it outside a developer machine.
 */
@Configuration
@Profile(NoAuthSecurityConfig.PROFILE)
public class NoAuthSecurityConfig {

    public static final String PROFILE = "no-auth";

    private static final Logger log = LoggerFactory.getLogger(NoAuthSecurityConfig.class);

    @Bean
    public SecurityFilterChain noAuthFilterChain(HttpSecurity http) {
        log.warn("Profile '{}' is active: authentication and authorization are DISABLED", PROFILE);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
