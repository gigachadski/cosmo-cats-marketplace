package com.cosmocats.marketplace.security;

import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For web slice tests: applies the real filter chains of the application and runs every
 * request as a signed-in administrator, so the tests can focus on controller behaviour.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, ProblemDetailSecurityHandler.class})
@WithMockUser(username = "luna-admin", roles = "ADMIN")
public @interface WithSecurityAsAdmin {
}
