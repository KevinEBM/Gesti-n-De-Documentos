package com.plantarsas.gestiondocumental.dashboard.controller;

import com.plantarsas.gestiondocumental.config.SecurityConfig;
import com.plantarsas.gestiondocumental.dashboard.dto.ActividadDocumentalResponse;
import com.plantarsas.gestiondocumental.dashboard.dto.DashboardAdminResponse;
import com.plantarsas.gestiondocumental.dashboard.dto.TipoActividad;
import com.plantarsas.gestiondocumental.dashboard.service.DashboardService;
import com.plantarsas.gestiondocumental.exception.GlobalExceptionHandler;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.JwtAccessDeniedHandler;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationEntryPoint;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationFilter;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.shared.enums.RolEnum;
import com.plantarsas.gestiondocumental.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DashboardControllerSecurityTest.ConfiguracionSeguridadDashboardTest.class)
@WebAppConfiguration
class DashboardControllerSecurityTest {

    private static final String URL_DASHBOARD = "/api/dashboard";

    private static final String URL_ACTIVIDAD_RECIENTE = "/api/dashboard/actividad-reciente";

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

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

    @AfterEach
    void limpiarMocks() {
        reset(dashboardService, jwtService, usuarioRepository);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor administradorAutenticado() {
        AuthenticatedUser administrador = new AuthenticatedUser(1L, "admin@plantarsas.com", RolEnum.ADMINISTRADOR);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                administrador, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(token);
    }

    private DashboardAdminResponse dashboardDePrueba() {
        return new DashboardAdminResponse(5L, 3L, 2L);
    }

    private ActividadDocumentalResponse actividadDePrueba() {
        return new ActividadDocumentalResponse(
                TipoActividad.NUEVA_VERSION, 1L, "PROC-001", "Título de prueba", 2,
                "Corrección de erratas", Instant.parse("2026-08-25T13:21:57Z"), 9L, "Ana", "Pérez"
        );
    }

    // ------------------------------------------------------------------
    // GET /api/dashboard
    // ------------------------------------------------------------------

    @Test
    void obtenerDashboard_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get(URL_DASHBOARD))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(dashboardService);
    }

    @Test
    void obtenerDashboard_conAdministrador_debeResponder200() throws Exception {
        when(dashboardService.obtenerDashboard()).thenReturn(dashboardDePrueba());

        mockMvc.perform(get(URL_DASHBOARD).with(administradorAutenticado()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void obtenerDashboard_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(get(URL_DASHBOARD))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void obtenerDashboard_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(get(URL_DASHBOARD))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    void obtenerDashboard_conExito_debeResponderConEstructuraApiResponseValida() throws Exception {
        when(dashboardService.obtenerDashboard()).thenReturn(dashboardDePrueba());

        MvcResult result = mockMvc.perform(get(URL_DASHBOARD).with(administradorAutenticado()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("exito").asBoolean()).isTrue();
        assertThat(json.get("datos").get("documentosPublicados").asLong()).isEqualTo(5L);
        assertThat(json.get("datos").get("usuariosActivos").asLong()).isEqualTo(3L);
        assertThat(json.get("datos").get("areasRegistradas").asLong()).isEqualTo(2L);
    }

    // ------------------------------------------------------------------
    // GET /api/dashboard/actividad-reciente
    // ------------------------------------------------------------------

    @Test
    void obtenerActividadReciente_sinAutenticacion_debeResponder401() throws Exception {
        mockMvc.perform(get(URL_ACTIVIDAD_RECIENTE))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(dashboardService);
    }

    @Test
    void obtenerActividadReciente_conAdministrador_debeResponder200() throws Exception {
        when(dashboardService.obtenerActividadReciente()).thenReturn(List.of(actividadDePrueba()));

        mockMvc.perform(get(URL_ACTIVIDAD_RECIENTE).with(administradorAutenticado()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void obtenerActividadReciente_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(get(URL_ACTIVIDAD_RECIENTE))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void obtenerActividadReciente_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(get(URL_ACTIVIDAD_RECIENTE))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    void obtenerActividadReciente_conExito_debeResponderConEstructuraApiResponseValida() throws Exception {
        when(dashboardService.obtenerActividadReciente()).thenReturn(List.of(actividadDePrueba()));

        MvcResult result = mockMvc.perform(get(URL_ACTIVIDAD_RECIENTE).with(administradorAutenticado()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("exito").asBoolean()).isTrue();
        assertThat(json.get("datos").get(0).get("tipoActividad").asText()).isEqualTo("NUEVA_VERSION");
        assertThat(json.get("datos").get(0).get("codigoDocumento").asText()).isEqualTo("PROC-001");
    }

    @Test
    void obtenerActividadReciente_sinActividad_debeResponderListaVacia() throws Exception {
        when(dashboardService.obtenerActividadReciente()).thenReturn(List.of());

        MvcResult result = mockMvc.perform(get(URL_ACTIVIDAD_RECIENTE).with(administradorAutenticado()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("datos").isArray()).isTrue();
        assertThat(json.get("datos")).isEmpty();
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            JwtAuthenticationEntryPoint.class,
            JwtAccessDeniedHandler.class,
            GlobalExceptionHandler.class,
            DashboardController.class
    })
    static class ConfiguracionSeguridadDashboardTest {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
