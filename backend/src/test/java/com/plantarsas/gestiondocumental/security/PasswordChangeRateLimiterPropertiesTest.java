package com.plantarsas.gestiondocumental.security;

import com.plantarsas.gestiondocumental.config.ClockConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ClockConfig.class, PasswordChangeRateLimiter.class})
@TestPropertySource(properties = {
        "security.password-change-rate-limit.max-attempts=2",
        "security.password-change-rate-limit.window-seconds=3600"
})
class PasswordChangeRateLimiterPropertiesTest {

    @Autowired
    private PasswordChangeRateLimiter limiter;

    @Test
    void debeRespetarMaxAttemptsDefinidoPorProperties() {
        Long usuarioId = 42L;

        limiter.registrarFallo(usuarioId);
        assertThat(limiter.estaBloqueado(usuarioId)).isFalse();

        limiter.registrarFallo(usuarioId);
        assertThat(limiter.estaBloqueado(usuarioId)).isTrue();
    }
}
