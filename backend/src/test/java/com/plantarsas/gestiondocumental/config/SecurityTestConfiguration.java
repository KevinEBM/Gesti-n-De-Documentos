package com.plantarsas.gestiondocumental.config;

import com.plantarsas.gestiondocumental.security.JwtAccessDeniedHandler;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationEntryPoint;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationFilter;
import com.plantarsas.gestiondocumental.security.LoginRateLimitFilter;
import com.plantarsas.gestiondocumental.security.LoginRateLimiter;
import com.plantarsas.gestiondocumental.security.PasswordChangeRateLimiter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        SecurityConfig.class,
        ClockConfig.class,
        LoginRateLimitFilter.class,
        LoginRateLimiter.class,
        PasswordChangeRateLimiter.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
public class SecurityTestConfiguration {
}
