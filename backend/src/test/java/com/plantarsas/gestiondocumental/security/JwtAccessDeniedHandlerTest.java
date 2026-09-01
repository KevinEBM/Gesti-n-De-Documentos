package com.plantarsas.gestiondocumental.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JwtAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JwtAccessDeniedHandler jwtAccessDeniedHandler =
            new JwtAccessDeniedHandler(objectMapper);

    @Test
    void handle_debeResponderConEstado403YCuerpoJsonDeError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException accessDeniedException =
                new AccessDeniedException("acceso denegado");

        assertThatCode(() ->
                jwtAccessDeniedHandler.handle(request, response, accessDeniedException)
        ).doesNotThrowAnyException();

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase(StandardCharsets.UTF_8.name());
        assertThat(response.getRedirectedUrl()).isNull();

        String cuerpo = response.getContentAsString();
        assertThat(cuerpo).isNotBlank();

        JsonNode json = objectMapper.readTree(cuerpo);
        assertThat(json.get("exito").asBoolean()).isFalse();
        assertThat(json.get("mensaje").asText()).isEqualTo("No tiene permisos para realizar esta operaci\u00F3n");
    }
}