package com.plantarsas.gestiondocumental.config;

import com.plantarsas.gestiondocumental.security.JwtAccessDeniedHandler;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationEntryPoint;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationFilter;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.ZoneOffset;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityConfigTest.ConfiguracionSeguridadTest.class)
@WebAppConfiguration
class SecurityConfigTest {

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Clock clock;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void postLogin_sinAutenticacion_debeResponder200() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk());
    }

    @Test
    void getLogin_sinAutenticacion_debeResponder401ConMensajeDeAutenticacionRequerida() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertRespuestaDeError(result, "Autenticación requerida");
    }

    @Test
    void getProtegida_sinAutenticacion_debeResponder401ConMensajeDeAutenticacionRequerida() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/prueba/protegida"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertRespuestaDeError(result, "Autenticación requerida");
    }

    @Test
    @WithMockUser
    void getProtegida_conUsuarioAutenticado_debeResponder200() throws Exception {
        mockMvc.perform(get("/api/prueba/protegida"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void postProtegida_conUsuarioAutenticadoSinCsrf_debeResponder200() throws Exception {
        mockMvc.perform(post("/api/prueba/protegida"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getProhibida_conUsuarioAutenticado_debeResponder403ConMensajeDePermisos() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/prueba/prohibida"))
                .andExpect(status().isForbidden())
                .andReturn();

        assertRespuestaDeError(result, "No tiene permisos para realizar esta operación");
    }

    @Test
    void clockBean_debeExistirYSerUTC() {
        assertThat(clock).isNotNull();
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void solicitudSinHeaderBearer_noDebeConsultarJwtServiceNiUsuarioRepository() throws Exception {
        mockMvc.perform(get("/api/prueba/protegida"))
                .andExpect(status().isUnauthorized());

        verify(jwtService, never()).esTokenValido(anyString());
        verifyNoInteractions(usuarioRepository);
    }

    private void assertRespuestaDeError(MvcResult result, String mensajeEsperado) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertThat(json.get("exito").asBoolean()).isFalse();
        assertThat(json.get("mensaje").asText()).isEqualTo(mensajeEsperado);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityConfig.class,
            ClockConfig.class,
            JwtAuthenticationFilter.class,
            JwtAuthenticationEntryPoint.class,
            JwtAccessDeniedHandler.class,
            PruebaController.class
    })
    static class ConfiguracionSeguridadTest {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @RestController
    static class PruebaController {

        @PostMapping("/api/auth/login")
        public ResponseEntity<Void> login() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/prueba/protegida")
        public ResponseEntity<Void> protegidaGet() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/prueba/protegida")
        public ResponseEntity<Void> protegidaPost() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/prueba/prohibida")
        public ResponseEntity<Void> prohibida() {
            throw new AccessDeniedException("acceso denegado");
        }
    }
}
