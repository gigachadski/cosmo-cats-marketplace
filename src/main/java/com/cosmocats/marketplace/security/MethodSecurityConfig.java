package com.cosmocats.marketplace.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/** Turns on {@code @PreAuthorize} checks on service methods (off in the "no-auth" profile). */
@Configuration
@Profile("!" + NoAuthSecurityConfig.PROFILE)
@EnableMethodSecurity
public class MethodSecurityConfig {
}
