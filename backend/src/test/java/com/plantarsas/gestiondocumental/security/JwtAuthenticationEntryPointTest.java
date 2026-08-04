package com.plantarsas.gestiondocumental.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint =
            new JwtAuthenticationEntryPoint(objectMapper);

    @Test
    void commence_debeResponderConEstado401YCuerpoJsonDeError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authenticationException =
                new BadCredentialsException("credenciales invalidas");

        assertThatCode(() ->
                jwtAuthenticationEntryPoint.commence(request, response, authenticationException)
        ).doesNotThrowAnyException();

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase(StandardCharsets.UTF_8.name());
        assertThat(response.getRedirectedUrl()).isNull();

        String cuerpo = response.getContentAsString();
        assertThat(cuerpo).isNotBlank();

        JsonNode json = objectMapper.readTree(cuerpo);
        assertThat(json.get("exito").asBoolean()).isFalse();
        assertThat(json.get("mensaje").asText()).isEqualTo("Autenticaci\u00F3n requerida");
    }
}