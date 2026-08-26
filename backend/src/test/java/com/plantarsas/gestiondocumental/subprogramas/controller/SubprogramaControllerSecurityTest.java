package com.plantarsas.gestiondocumental.subprogramas.controller;

import com.plantarsas.gestiondocumental.config.SecurityConfig;
import com.plantarsas.gestiondocumental.security.AuthenticatedUser;
import com.plantarsas.gestiondocumental.security.JwtAccessDeniedHandler;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationEntryPoint;
import com.plantarsas.gestiondocumental.security.JwtAuthenticationFilter;
import com.plantarsas.gestiondocumental.security.JwtService;
import com.plantarsas.gestiondocumental.subprogramas.dto.SubprogramaResponse;
import com.plantarsas.gestiondocumental.subprogramas.service.SubprogramaService;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SubprogramaControllerSecurityTest.ConfiguracionSeguridadSubprogramasTest.class)
@WebAppConfiguration
class SubprogramaControllerSecurityTest {

    private static final String SUBPROGRAMA_REQUEST_JSON =
            "{\"codigo\":\"SUB\",\"nombre\":\"Subprograma A\",\"descripcion\":\"Descripcion\",\"areaId\":1}";

    private static final String SUBPROGRAMA_UPDATE_REQUEST_JSON =
            "{\"codigo\":\"SUB\",\"nombre\":\"Subprograma A\",\"descripcion\":\"Descripcion\",\"areaId\":1}";

    private static final String SUBPROGRAMA_ESTADO_REQUEST_JSON =
            "{\"activo\":false}";

    @MockitoBean
    private SubprogramaService subprogramaService;

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
        reset(subprogramaService, jwtService, usuarioRepository);
    }

    private SubprogramaResponse respuestaDePrueba(Long id) {
        return new SubprogramaResponse(
                id,
                "SUB",
                "Subprograma A",
                "Descripcion",
                null,
                true,
                null,
                null
        );
    }

    private void assertRespuestaDeError(MvcResult result, String mensajeEsperado) throws Exception {
        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertThat(json.get("exito").asBoolean()).isFalse();
        assertThat(json.get("mensaje").asText()).isEqualTo(mensajeEsperado);
    }

    private RequestPostProcessor usuarioAutenticado(RolEnum rol) {
        AuthenticatedUser usuario = new AuthenticatedUser(4L, "usuario@plantarsas.com", rol);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                usuario,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(token);
    }

    @Test
    void crear_sinAutenticacion_debeResponder401() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/subprogramas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBPROGRAMA_REQUEST_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertRespuestaDeError(result, "Autenticación requerida");
        verifyNoInteractions(subprogramaService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void crear_conAdministrativo_debeResponder403() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/subprogramas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBPROGRAMA_REQUEST_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        assertRespuestaDeError(result, "No tiene permisos para realizar esta operación");
        verifyNoInteractions(subprogramaService);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void crear_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(post("/api/subprogramas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBPROGRAMA_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subprogramaService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void crear_conAdministrador_debeResponder201() throws Exception {
        when(subprogramaService.crear(any())).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(post("/api/subprogramas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBPROGRAMA_REQUEST_JSON))
                .andExpect(status().isCreated());

        verify(subprogramaService).crear(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void listar_conAdministrativo_debeResponder200() throws Exception {
        when(subprogramaService.listarParaUsuario(any())).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/subprogramas").with(usuarioAutenticado(RolEnum.ADMINISTRATIVO)))
                .andExpect(status().isOk());

        verify(subprogramaService).listarParaUsuario(any());
        verify(subprogramaService, never()).listar();
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void listar_conJefeArea_debeResponder200() throws Exception {
        when(subprogramaService.listarParaUsuario(any())).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/subprogramas").with(usuarioAutenticado(RolEnum.JEFE_AREA)))
                .andExpect(status().isOk());

        verify(subprogramaService).listarParaUsuario(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void listar_conAdministrador_debeResponder200() throws Exception {
        when(subprogramaService.listarParaUsuario(any())).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/subprogramas").with(usuarioAutenticado(RolEnum.ADMINISTRADOR)))
                .andExpect(status().isOk());

        verify(subprogramaService).listarParaUsuario(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void obtenerPorId_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(get("/api/subprogramas/1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subprogramaService);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void obtenerPorId_conAdministrador_debePermitirAcceso() throws Exception {
        when(subprogramaService.obtenerPorId(1L)).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(get("/api/subprogramas/1"))
                .andExpect(status().isOk());

        verify(subprogramaService).obtenerPorId(1L);
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void actualizar_conJefeArea_debeResponder403() throws Exception {
        mockMvc.perform(put("/api/subprogramas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBPROGRAMA_UPDATE_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(subprogramaService, never()).actualizar(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void actualizar_conAdministrador_debePermitirAcceso() throws Exception {
        when(subprogramaService.actualizar(eq(1L), any())).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(put("/api/subprogramas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBPROGRAMA_UPDATE_REQUEST_JSON))
                .andExpect(status().isOk());

        verify(subprogramaService).actualizar(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void cambiarEstado_conAdministrativo_debeResponder403() throws Exception {
        mockMvc.perform(patch("/api/subprogramas/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBPROGRAMA_ESTADO_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(subprogramaService, never()).cambiarEstado(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void cambiarEstado_conAdministrador_debePermitirAcceso() throws Exception {
        when(subprogramaService.cambiarEstado(eq(1L), any())).thenReturn(respuestaDePrueba(1L));

        mockMvc.perform(patch("/api/subprogramas/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SUBPROGRAMA_ESTADO_REQUEST_JSON))
                .andExpect(status().isOk());

        verify(subprogramaService).cambiarEstado(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void listarActivosPorArea_conAdministrador_debeResponder200() throws Exception {
        when(subprogramaService.listarActivosPorArea(eq(1L), any())).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/subprogramas/area/1/activos").with(usuarioAutenticado(RolEnum.ADMINISTRADOR)))
                .andExpect(status().isOk());

        verify(subprogramaService).listarActivosPorArea(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "JEFE_AREA")
    void listarActivosPorArea_conJefeArea_debeResponder200() throws Exception {
        when(subprogramaService.listarActivosPorArea(eq(1L), any())).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/subprogramas/area/1/activos").with(usuarioAutenticado(RolEnum.JEFE_AREA)))
                .andExpect(status().isOk());

        verify(subprogramaService).listarActivosPorArea(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void listarActivosPorArea_conAdministrativo_debeResponder200() throws Exception {
        when(subprogramaService.listarActivosPorArea(eq(1L), any())).thenReturn(List.of(respuestaDePrueba(1L)));

        mockMvc.perform(get("/api/subprogramas/area/1/activos").with(usuarioAutenticado(RolEnum.ADMINISTRATIVO)))
                .andExpect(status().isOk());

        verify(subprogramaService).listarActivosPorArea(eq(1L), any());
    }

    @Test
    void listarActivosPorArea_sinAutenticacion_debeResponder401() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/subprogramas/area/1/activos"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertRespuestaDeError(result, "Autenticación requerida");
        verifyNoInteractions(subprogramaService);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            JwtAuthenticationEntryPoint.class,
            JwtAccessDeniedHandler.class,
            SubprogramaController.class
    })
    static class ConfiguracionSeguridadSubprogramasTest {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
